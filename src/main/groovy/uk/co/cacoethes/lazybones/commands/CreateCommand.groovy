package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import joptsimple.OptionParser
import org.codehaus.groovy.control.CompilerConfiguration
import uk.co.cacoethes.lazybones.BintrayPackageSource
import uk.co.cacoethes.lazybones.LazybonesMain
import uk.co.cacoethes.lazybones.LazybonesScript
import uk.co.cacoethes.lazybones.PackageInfo
import uk.co.cacoethes.lazybones.PackageSource
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

    @Override
    String getName() { return "create" }

    @Override
    String getDescription() {
        return "Creates a new project from a template."
    }

    @Override
    int execute(List<String> args, Map globalOptions, ConfigObject configuration) {
        def cmdOptions = parseArguments(args, 2..3)
        if (!cmdOptions) return 1

        // Inject the latest version into the args list if the user hasn't
        // provided it.
        String packageName = args[0]
        PackageInfo pkgInfo = null
        PackageSource packageSource = null

        for (String bintrayRepoName in configuration.bintrayRepositories) {
            log.fine "Searching for ${packageName} in ${bintrayRepoName}"
            packageSource = new BintrayPackageSource(bintrayRepoName)
            pkgInfo = packageSource.fetchPackageInfo(packageName)

            if (pkgInfo) {
                log.fine "Found!"
                break
            }
        }

        if (!pkgInfo) {
            log.severe "Cannot find a template named '${packageName}'. Project has not been created."
            return 1
        }

        File targetDir
        String requestedVersion
        if (args.size() == 2) {
            // No version specified, so pull the latest from the package server.
            targetDir = args[1] as File
            requestedVersion = pkgInfo.latestVersion
        }
        else {
            targetDir = args[2] as File
            requestedVersion = args[1]
        }

        log.fine "Attempting to download version ${requestedVersion} of template '${packageName}' into ${targetDir.path}"

        // Can't fetch the latest version until Bintray allows anonymous API access.
        // Or I set up a separate server for this stuff.
        def templateZip = fetchTemplate(packageName,
                requestedVersion,
                packageSource.getTemplateUrl(packageName, requestedVersion))

        if (!templateZip) {
            log.severe "Cannot find version ${requestedVersion} of template '${packageName}'. Project has not been created."
            return 1
        }

        targetDir.mkdirs()
        ArchiveMethods.unzip(templateZip, targetDir)

        // Run the post-install script if it exists. The user can pass variables
        // to the script via -P command line arguments. This also places
        // lazybonesVersion, lazybonesMajorVersion, and lazybonesMinorVersion
        // variables into the script binding.
        try {
            def scriptVariables = cmdOptions.valuesOf("P").collectEntries { String it -> it.split('=') as List }
            scriptVariables << evaluateVersionScriptVariables()
            runPostInstallScript(targetDir, scriptVariables)
        }
        catch (Throwable throwable) {
            log.warning "Post install script caused an exception, project might be corrupt: ${throwable.message}"

            if (globalOptions.stacktrace) {
                log.log Level.SEVERE, "", throwable
            }
            return 1
        }

        // Find a suitable README and display that if it exists.
        def readmeFiles = targetDir.listFiles({ File dir, String name ->
            name == "README" || name.startsWith("README")
        } as FilenameFilter)

        log.info ""
        if (!readmeFiles) log.info "This project has no README"
        else log.info readmeFiles[0].text

        log.info ""
        log.info "Project created in ${targetDir.path}!"

        return 0
    }

    /**
     * Reads the Lazybones version, breaks it up, and adds {@code lazybonesVersion},
     * {@code lazybonesMajorVersion}, and {@code lazybonesMinorVersion} variables
     * to a map that is then returned.
     */
    protected Map evaluateVersionScriptVariables() {
        def version = LazybonesMain.readVersion()
        def vars = [lazybonesVersion: version]

        def versionParts = version.split(/\./)
        assert versionParts.size() > 1

        vars["lazybonesMajorVersion"] = versionParts[0]?.toInteger()
        vars["lazybonesMinorVersion"] = versionParts[1]?.toInteger()

        return vars
    }

    @Override
    protected OptionParser createParser() {
        def parser = new OptionParser()
        parser.accepts("spaces", "Sets the number of spaces to use for indent in files.").withRequiredArg()
        parser.accepts("P", "Add a substitution variable for file filtering.").withRequiredArg()
        return parser
    }

    @Override
    protected String getUsage() { return USAGE }

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

    protected File fetchTemplate(String templateName, String requestedVersion, String externalUrl) {
        // Does it exist in the cache? If not, pull it from Bintray.
        def packageFile = new File(INSTALL_DIR, "${templateName}-${requestedVersion}.zip")

        if (!packageFile.exists()) {
            INSTALL_DIR.mkdirs()
            try {
                packageFile.withOutputStream { OutputStream out ->
                    new URL(externalUrl).withInputStream { InputStream input ->
                        out << input
                    }
                }
            }
            catch (all) {
                log.log Level.SEVERE, "${externalUrl} was not found.", all
                packageFile.deleteOnExit()
                return null
            }
        }

        return packageFile
    }

}
