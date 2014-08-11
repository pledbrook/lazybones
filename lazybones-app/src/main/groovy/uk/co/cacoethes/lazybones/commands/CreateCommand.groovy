package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import joptsimple.OptionParser
import joptsimple.OptionSet
import uk.co.cacoethes.lazybones.LazybonesScriptException
import uk.co.cacoethes.lazybones.NoVersionsFoundException
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.lazybones.packagesources.PackageSource
import uk.co.cacoethes.lazybones.packagesources.PackageSourceBuilder
import uk.co.cacoethes.lazybones.scm.GitAdapter
import uk.co.cacoethes.util.ArchiveMethods
import wslite.http.HTTPClientException

import java.util.logging.Level

/**
 * Implements Lazybone's create command, which creates a new project based on
 * a specified template.
 */
@Log
class CreateCommand extends AbstractCommand {
    final PackageSourceBuilder packageSourceFactory
    final PackageLocationBuilder packageLocationFactory
    final PackageDownloader packageDownloader
    final Map mappings

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
    private static final String GIT_OPT = "with-git"

    CreateCommand(Configuration config) {
        this(config.settings.cache.dir as File)
        assert config.settings.cache.dir
        mappings = config.settings.templates.mappings
    }

    CreateCommand(File cacheDir) {
        packageSourceFactory = new PackageSourceBuilder()
        packageLocationFactory = new PackageLocationBuilder(cacheDir)
        packageDownloader = new PackageDownloader()
    }

    @Override
    String getName() { return "create" }

    @Override
    String getDescription() {
        return "Creates a new project from a template."
    }

    @Override
    protected OptionParser doAddToParser(OptionParser parser) {
        parser.accepts(SPACES_OPT, "Sets the number of spaces to use for indent in files.").withRequiredArg()
        parser.accepts(VAR_OPT, "Add a substitution variable for file filtering.").withRequiredArg()
        parser.accepts(GIT_OPT, "Creates a git repository in the new project.")
        return parser
    }

    @Override
    protected IntRange getParameterRange() {
        2..3  // Either a directory or a version + a directory
    }

    @Override
    protected String getUsage() { return USAGE }

    protected int doExecute(OptionSet cmdOptions, Map globalOptions, Configuration configuration) {
        try {
            def createData = evaluateArgs(cmdOptions)

            List<PackageSource> packageSources = packageSourceFactory.buildPackageSourceList(configuration)
            PackageLocation packageLocation = packageLocationFactory.buildPackageLocation(
                    createData.packageArg.templateName,
                    createData.requestedVersion,
                    packageSources)
            File pkg = packageDownloader.downloadPackage(
                    packageLocation,
                    createData.packageArg.templateName,
                    createData.requestedVersion)

            def targetDir = createData.targetDir.canonicalFile
            targetDir.mkdirs()
            ArchiveMethods.unzip(pkg, targetDir)

            def scmAdapter = null
            if (cmdOptions.has(GIT_OPT)) scmAdapter = new GitAdapter(configuration)

            def executor = new InstallationScriptExecuter(scmAdapter)
            executor.runPostInstallScriptWithArgs(
                    cmdOptions.valuesOf(VAR_OPT).collectEntries { String it -> it.split('=') as List },
                    createData.packageArg.qualifiers,
                    targetDir)

            logReadme(createData)

            logSuccess(createData)

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
            log.severe "No version of '${ex.packageName}' has been published. This can also happen if " +
                    "the latest version on Bintray is 'null'."
            return 1
        }
        catch (HTTPClientException ex) {
            if (OfflineMode.isOffline(ex)) {
                OfflineMode.printlnOfflineMessage(ex, log, globalOptions.stacktrace as boolean)
            }
            else {
                log.severe "Unexpected failure: ${ex.message}"
                if (globalOptions.stacktrace) log.log Level.SEVERE, "", ex
            }

            println()
            println "Cannot create a new project when the template isn't locally cached or no version is specified"
            return 1
        }
        catch (LazybonesScriptException ex) {
            log.severe "Post install script caused an exception, project might be corrupt: ${ex.cause.message}"
            log.severe "The unpacked template will remain in place to help you diagnose the problem"

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

    protected CreateCommandInfo evaluateArgs(OptionSet commandOptions) {
        def mainArgs = commandOptions.nonOptionArguments()
        def createCmdInfo = getCreateInfoFromArgs(mainArgs)

        logStart createCmdInfo.packageArg.templateName, createCmdInfo.requestedVersion, createCmdInfo.targetDir

        return createCmdInfo
    }

    @SuppressWarnings('SpaceAroundOperator')
    protected CreateCommandInfo getCreateInfoFromArgs(List<String> mainArgs) {

        def packageName = new TemplateArg(mappings?."${mainArgs[0]}" ?: mainArgs[0])

        if (hasVersionArg(mainArgs)) {
            return new CreateCommandInfo(packageName, mainArgs[1], toFile(mainArgs[2]))
        }

        return new CreateCommandInfo(packageName, '', toFile(mainArgs[1]))
    }

    protected boolean hasVersionArg(List<String> args) {
        return args.size() == 3
    }

    private void logStart(String packageName, String version, File targetPath) {
        if (log.isLoggable(Level.INFO)) {
            log.info "Creating project from template " + packageName + ' ' +
                    (version ?: "(latest)") + " in " +
                    (isPathCurrentDirectory(targetPath) ? "current directory" : "'${targetPath}'")
        }
    }

    private void logSuccess(CreateCommandInfo createData) {
        log.info ""
        log.info "Project created in " + (isPathCurrentDirectory(createData.targetDir) ?
            'current directory' : createData.targetDir.path) + '!'
    }

    @SuppressWarnings('SpaceBeforeOpeningBrace')
    private void logReadme(CreateCommandInfo createData) {
        // Find a suitable README and display that if it exists.
        def readmeFiles = createData.targetDir.canonicalFile.listFiles({ File dir, String name ->
            name == README_BASENAME || name.startsWith(README_BASENAME)
        } as FilenameFilter)

        log.info ""
        if (!readmeFiles) log.info "This project has no README"
        else log.info readmeFiles[0].text
    }

    private boolean isPathCurrentDirectory(File path) {
        return path.canonicalPath == new File("").canonicalPath
    }

    /**
     * Converts a string file path to a `File` instance. Its unique behaviour
     * is that the path "." is translated to "", i.e. the empty path.
     */
    private File toFile(String path) {
        return new File(path == "." ? "" : path)
    }
}
