package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import joptsimple.OptionParser
import joptsimple.OptionSet
import org.apache.commons.io.FilenameUtils
import uk.co.cacoethes.lazybones.LazybonesScriptException
import uk.co.cacoethes.lazybones.NoVersionsFoundException
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.packagesources.PackageSource
import uk.co.cacoethes.lazybones.packagesources.PackageSourceBuilder
import uk.co.cacoethes.util.ArchiveMethods

import java.util.logging.Level

/**
 * Implements Lazybone's create command, which creates a new project based on
 * a specified template.
 */
@CompileStatic
@Log
class CreateCommand extends AbstractCommand {
    final PackageSourceBuilder packageSourceFactory = new PackageSourceBuilder()
    final PackageLocationBuilder packageLocationFactory = new PackageLocationBuilder()
    final PackageDownloader packageDownloader = new PackageDownloader()
    final InstallationScriptExecuter installationScriptExecuter = new InstallationScriptExecuter()

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
    protected static final String VAR_OPT = "P"
    protected static final String GIT_OPT = "with-git"

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
        1..3
    }

    @Override
    protected String getUsage() { return USAGE }

    protected int doExecute(OptionSet cmdOptions,  Map globalOptions, ConfigObject configuration) {
        try {
            def createData = evaluateArgs(cmdOptions)

            List<PackageSource> packageSources = packageSourceFactory.buildPackageSourceList(configuration)
            PackageLocation packageLocation = packageLocationFactory.buildPackageLocation(createData, packageSources)
            File pkg = packageDownloader.downloadPackage(packageLocation, createData)

            createData.targetDir.mkdirs()
            ArchiveMethods.unzip(pkg, createData.targetDir)

            installationScriptExecuter.runPostInstallScriptWithArgs(cmdOptions, createData)

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

    protected CreateCommandInfo evaluateArgs(OptionSet commandOptions) {
        def mainArgs = commandOptions.nonOptionArguments()
        def createCmdInfo = getCreateInfoFromArgs(mainArgs)

        logStart createCmdInfo.packageName, createCmdInfo.requestedVersion, createCmdInfo.targetDir.path

        return createCmdInfo
    }

    private CreateCommandInfo getCreateInfoFromArgs(List<String> mainArgs) {
        if (hasVersionArg(mainArgs)) {
            return new CreateCommandInfo(mainArgs[0], mainArgs[1], mainArgs[2] as File)
        }

        return new CreateCommandInfo(mainArgs[0], '', mainArgs[1] as File)
    }

    protected boolean hasVersionArg(List<String> args) {
        return args.size() == 3
    }


    private void logStart(String packageName, String version, String targetPath) {
        if (log.isLoggable(Level.INFO)) {
            log.info "Creating project from template " + packageName + ' ' +
                    (version ?: "(latest)") + " in " +
                    (isPathCurrentDirectory(targetPath) ? "current directory" : "'${targetPath}'")
        }
    }

    private void logSuccess(CreateCommandInfo createData) {
        log.info ""
        log.info "Project created in " + (isPathCurrentDirectory(createData.targetDir.path) ?
                'current directory' : createData.targetDir.path) + '!'
    }

    private void logReadme(CreateCommandInfo createData) {
        // Find a suitable README and display that if it exists.
        def readmeFiles = createData.targetDir.listFiles( { File dir, String name ->
            name == README_BASENAME || name.startsWith(README_BASENAME)
        } as FilenameFilter)

        log.info ""
        if (!readmeFiles) log.info "This project has no README"
        else log.info readmeFiles[0].text
    }

    private boolean isPathCurrentDirectory(String path) {
        return FilenameUtils.equalsNormalized(path, ".")
    }
}