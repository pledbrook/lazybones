package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import joptsimple.OptionParser
import joptsimple.OptionSet
import org.apache.commons.io.FileUtils
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.lazybones.LazybonesScriptException
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.util.ArchiveMethods

import java.util.logging.Level

/**
 * Implements Lazybone's generate command, which processes subtemplates in a
 * Lazybones-created project. The command unpacks the subtemplate into the
 * project's .lazybones directory and runs the post-install script. It's up
 * to that script to create directories and files in main project source tree.
 */
@Log
class GenerateCommand extends AbstractCommand {
    static final File LAZYBONES_DIR = new File(".lazybones")

    final Map mappings

    static final String USAGE = """\
USAGE: generate <template>

  where  template = The name of the subtemplate to use.
"""
    private static final String SPACES_OPT = "spaces"
    private static final String VAR_OPT = "P"

    @Override
    String getName() { return "generate" }

    @Override
    String getDescription() {
        return "Generates new files in the current project based on a subtemplate."
    }

    @Override
    protected OptionParser doAddToParser(OptionParser parser) {
        parser.accepts(SPACES_OPT, "Sets the number of spaces to use for indent in files.").withRequiredArg()
        parser.accepts(VAR_OPT, "Add a substitution variable for file filtering.").withRequiredArg()
        return parser
    }

    @Override
    protected IntRange getParameterRange() {
        1..1 // Just the subtemplate name
    }

    @Override
    protected String getUsage() { return USAGE }

    @Override
    protected int doExecute(OptionSet cmdOptions, Map globalOptions, Configuration configuration) {
        // Make sure this is a Lazybones-created project, otherwise there are
        // no subtemplates to use.
        if (!LAZYBONES_DIR.exists()) {
            log.severe "You cannot use `generate` here: this is not a Lazybones-created project"
            return 1
        }

        try {
            def arg = new TemplateArg(cmdOptions.nonOptionArguments()[0])

            def outDir = new File(LAZYBONES_DIR, "${arg.templateName}-unpacked")
            outDir.mkdirs()
            ArchiveMethods.unzip(templateNameToPackageFile(arg.templateName), outDir)

            def executor = new InstallationScriptExecuter()
            executor.runPostInstallScriptWithArgs(
                    cmdOptions.valuesOf(VAR_OPT).collectEntries { String it -> it.split('=') as List },
                    arg.qualifiers,
                    new File("."),
                    outDir)

            FileUtils.deleteDirectory(outDir)

            return 0
        }
        catch (PackageNotFoundException ex) {
            log.severe ex.message
            return 1
        }
        catch (LazybonesScriptException ex) {
            log.severe "Post install script caused an exception, project might be corrupt: ${ex.cause.message}"

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

    @SuppressWarnings("SpaceBeforeOpeningBrace")
    protected File templateNameToPackageFile(String name) {
        def matchingFiles = LAZYBONES_DIR.listFiles({ File f ->
            f.name ==~ /^${name}\-template\-.*\.zip$/
        } as FileFilter)

        if (!matchingFiles) throw new PackageNotFoundException("Cannot find a subtemplate named '$name'")
        return matchingFiles[0]
    }
}
