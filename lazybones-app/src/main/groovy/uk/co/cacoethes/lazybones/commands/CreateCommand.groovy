package uk.co.cacoethes.lazybones.commands

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import joptsimple.OptionParser
import joptsimple.OptionSet
import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.control.CompilerConfiguration
import uk.co.cacoethes.lazybones.BintrayPackageSource
import uk.co.cacoethes.lazybones.LazybonesMain
import uk.co.cacoethes.lazybones.LazybonesScript
import uk.co.cacoethes.lazybones.LazybonesScriptException
import uk.co.cacoethes.lazybones.NoVersionsFoundException
import uk.co.cacoethes.lazybones.PackageInfo
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.util.ArchiveMethods

import java.util.logging.Level

/**
 * Implements Lazybone's create command, which creates a new project based on
 * a specified template.
 */
@CompileStatic
@Log
class CreateCommand extends AbstractCommand {
    /** Where the template packages are stored/cached */
    static final File INSTALL_DIR = new File(System.getProperty('user.home'), ".lazybones/templates")

    static final String USAGE = """\
USAGE: create <template> <version>? <dir>

  where  template = The name of the project template to use.
         version  = (optional) The version of the project template to use. Uses
                    the latest version of the template by default.
         dir      = The name of the directory in which to create the project
                    structure. This can be '.' to mean 'in the current
                    directory.'
"""
    private static final String README_BASENAME = "README"
    private static final String SPACES_OPT = "spaces"
    private static final String VAR_OPT = "P"

    @Override
    String getName() { return "create" }

    @Override
    String getDescription() {
        return "Creates a new project from a template."
    }

    @Override
    protected IntRange getParameterRange() {
        1..3
    }

    @Override
    protected String getUsage() { return USAGE }

    protected int doExecute(OptionSet cmdOptions,  Map globalOptions, ConfigObject configuration) {

        List<String> repositoryList = (List) configuration.bintrayRepositories

        try {
            def createData = evaluateArgs(cmdOptions, repositoryList)
            def pkg = fetchTemplatePackage(createData, repositoryList)

            createData.targetDir.mkdirs()
            ArchiveMethods.unzip(pkg, createData.targetDir)

            runPostInstallScriptWithArgs(cmdOptions, createData)

            // Find a suitable README and display that if it exists.
            def readmeFiles = createData.targetDir.listFiles( { File dir, String name ->
                name == README_BASENAME || name.startsWith(README_BASENAME)
            } as FilenameFilter)

            log.info ""
            if (!readmeFiles) log.info "This project has no README"
            else log.info readmeFiles[0].text

            log.info ""
            log.info "Project created in " + (isPathCurrentDirectory(createData.targetDir.path) ?
                        'current directory' : createData.targetDir.path) + '!'

            return 0
        }
        catch (PackageNotFoundException ex) {
            if (ex.version) {
                log.severe "Cannot find version ${ex.version} of template '${ex.name}'. Project has not been created."
            }
            else {
                log.severe "Cannot find a template named '${ex.name}'. Project has not been created."
            }
            return 1
        }
        catch (NoVersionsFoundException ex) {
            log.severe "No version of '${ex.packageName}' has been published"
            return 1
        }
        catch (LazybonesScriptException ex) {
            log.warning "Post install script caused an exception, project might be corrupt: ${ex.cause.message}"
            log.warning "The unpacked template will remain in place to help you diagnose the problem"

            if (globalOptions.stacktrace) {
                log.log Level.SEVERE, "", ex.cause
            }

            return 1
        }
        catch (all) {
            log.log Level.SEVERE, "", all
            return 1
        }
    }

    protected CreateCommandInfo evaluateArgs(OptionSet commandOptions, List<String> repositories) {
        def mainArgs = commandOptions.nonOptionArguments()
        def createCmdInfo = getCreateInfoFromArgs(mainArgs)

        logStart createCmdInfo.packageName, createCmdInfo.requestedVersion, createCmdInfo.targetDir.path

        if (!createCmdInfo.requestedVersion) {
            // No version specified, so pull the latest from the package server.
            createCmdInfo.requestedVersion = getPackageInfo(createCmdInfo.packageName, repositories).latestVersion
        }

        return createCmdInfo
    }

    private CreateCommandInfo getCreateInfoFromArgs(List<String> mainArgs) {
        if (hasVersionArg(mainArgs)) {
            return new CreateCommandInfo(mainArgs[0], mainArgs[1], mainArgs[2] as File)
        }
        else {
            return new CreateCommandInfo(mainArgs[0], '', mainArgs[1] as File)
        }
    }

    protected void runPostInstallScriptWithArgs(OptionSet cmdOptions, CreateCommandInfo createData) {
        // Run the post-install script if it exists. The user can pass variables
        // to the script via -P command line arguments. This also places
        // lazybonesVersion, lazybonesMajorVersion, and lazybonesMinorVersion
        // variables into the script binding.
        try {
            def scriptVariables = cmdOptions.valuesOf(VAR_OPT).collectEntries { String it -> it.split('=') as List }
            scriptVariables << evaluateVersionScriptVariables()
            runPostInstallScript(createData.targetDir, scriptVariables)
        }
        catch (all) {
            throw new LazybonesScriptException(all)
        }
    }

    /**
     * Runs the post install script if it exists in the unpacked template
     * package. Once the script has been run, it is deleted.
     * @param targetDir the target directory that contains the lazybones.groovy script
     * @param model a map of variables available to the script
     * @return the lazybones script if it exists
     */
    protected Script runPostInstallScript(File targetDir, Map<String, String> model) {
        def file = new File(targetDir, "lazybones.groovy")
        if (file.exists()) {
            def compiler = new CompilerConfiguration()
            compiler.scriptBaseClass = LazybonesScript.name

            // Can't use 'this' here because the static type checker does not
            // treat it as the class instance:
            //       https://jira.codehaus.org/browse/GROOVY-6162
            def shell = new GroovyShell(getClass().classLoader, new Binding(model), compiler)

            LazybonesScript script = shell.parse(file) as LazybonesScript
            script.targetDir = targetDir.path
            script.run()
            file.delete()
            return script
        }

        return null
    }

    /**
     * Reads the Lazybones version, breaks it up, and adds {@code lazybonesVersion},
     * {@code lazybonesMajorVersion}, and {@code lazybonesMinorVersion} variables
     * to a map that is then returned.
     */
    protected Map evaluateVersionScriptVariables() {
        def version = LazybonesMain.readVersion()
        def vars = [lazybonesVersion: version]

        def versionParts = version.split(/[\.\-]/)
        assert versionParts.size() > 1

        vars["lazybonesMajorVersion"] = versionParts[0]?.toInteger()
        vars["lazybonesMinorVersion"] = versionParts[1]?.toInteger()

        return vars
    }

    @Override
    protected OptionParser doAddToParser(OptionParser parser) {
        parser.accepts(SPACES_OPT, "Sets the number of spaces to use for indent in files.").withRequiredArg()
        parser.accepts(VAR_OPT, "Add a substitution variable for file filtering.").withRequiredArg()
        return parser
    }

    protected File fetchTemplatePackage(CreateCommandInfo info, List<String> repositories) {
        // Does it exist in the cache? If not, pull it from Bintray.
        def packageFile = new File(INSTALL_DIR, "${info.packageName}-${info.requestedVersion}.zip")

        if (!packageFile.exists()) {
            INSTALL_DIR.mkdirs()

            // The package info may not have been requested yet. It depends on
            // whether the user specified a specific version or not. Hence we
            // try to fetch the package info first and only throw an exception
            // if it's still null.
            //
            // There is an argument for having getPackageInfo() throw the exception
            // itself. May still do that.
            log.fine "${info.packageName} ${info.requestedVersion} is not cached locally." +
                    " Searching the repositories for it."
            if (!info.packageInfo) info.packageInfo = getPackageInfo(info.packageName, repositories)

            def templateUrl = info.packageInfo.source.getTemplateUrl(info.packageName, info.requestedVersion)
            log.fine "Attempting to download version ${info.requestedVersion} " +
                    "of template '${info.packageName}' into ${INSTALL_DIR}"

            try {
                packageFile.withOutputStream { OutputStream out ->
                    new URL(templateUrl).withInputStream { InputStream input ->
                        out << input
                    }
                }
            }
            catch (FileNotFoundException ex) {
                packageFile.deleteOnExit()
                throw new PackageNotFoundException(info.packageName, info.requestedVersion, ex)
            }
            catch (all) {
                packageFile.deleteOnExit()
                throw all
            }
        }

        return packageFile
    }

    protected boolean hasVersionArg(List<String> args) {
        return args.size() == 3
    }

    protected PackageInfo getPackageInfo(String packageName, List<String> repositories) {
        for (String bintrayRepoName in repositories) {
            log.fine "Searching for ${packageName} in ${bintrayRepoName}"

            def pkgInfo = new BintrayPackageSource(bintrayRepoName).fetchPackageInfo(packageName)
            if (pkgInfo) {
                log.fine "Found!"
                return pkgInfo
            }
        }

        throw new PackageNotFoundException(packageName)
    }

    private void logStart(String packageName, String version, String targetPath) {
        if (log.isLoggable(Level.INFO)) {
            log.info "Creating project from template " + packageName + ' ' +
                    (version ? version : "(latest)") + " in " +
                    (isPathCurrentDirectory(targetPath) ? "current directory" : "'${targetPath}'")
        }
    }

    private boolean isPathCurrentDirectory(String path) {
        return FilenameUtils.equalsNormalized(path, ".")
    }
}

@Canonical
class CreateCommandInfo {
    String packageName
    String requestedVersion
    File targetDir
    PackageInfo packageInfo
}
