package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import joptsimple.OptionParser
import joptsimple.OptionSet
import org.apache.commons.io.FileUtils
import uk.co.cacoethes.lazybones.LazybonesScriptException
import uk.co.cacoethes.util.ArchiveMethods

import java.util.logging.Level

/**
 * Implements Lazybone's create command, which creates a new project based on
 * a specified template.
 */
@Log
class GenerateCommand extends AbstractCommand {
    final Map mappings

    static final String USAGE = """\
USAGE: generate <template>

  where  template = The name of the sub-template to use.
"""
    private static final String README_BASENAME = "README"
    private static final String SPACES_OPT = "spaces"
    protected static final String VAR_OPT = "P"
    protected static final String GIT_OPT = "with-git"

    @Override
    String getName() { return "generate" }

    @Override
    String getDescription() {
        return "Generates new files in the current project based on a sub-template."
    }

    @Override
    protected OptionParser doAddToParser(OptionParser parser) {
        parser.accepts(SPACES_OPT, "Sets the number of spaces to use for indent in files.").withRequiredArg()
        parser.accepts(VAR_OPT, "Add a substitution variable for file filtering.").withRequiredArg()
        return parser
    }

    @Override
    protected IntRange getParameterRange() {
        1..1 // Just the sub-template name
    }

    @Override
    protected String getUsage() { return USAGE }

    protected int doExecute(OptionSet cmdOptions, Map globalOptions, ConfigObject configuration) {
        try {
            def tmplName = getTemplateName(cmdOptions)
            def outDir = new File(".lazybones/${tmplName}-unpacked")
            outDir.mkdirs()
            ArchiveMethods.unzip(templateNameToPackageFile(tmplName), outDir)

            def executor = new InstallationScriptExecuter()
            executor.runPostInstallScriptWithArgs(cmdOptions, new File(""), outDir)

            FileUtils.deleteDirectory(outDir)

            return 0
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

    protected String getTemplateName(OptionSet commandOptions) {
        return commandOptions.nonOptionArguments()[0]
    }

    protected File templateNameToPackageFile(String name) {
        return new File(".lazybones", name + "-subtemplate.zip")
    }

    private void logStart(String packageName) {
        if (log.isLoggable(Level.INFO)) {
            log.info "Generating from sub-template " + packageName
        }
    }

    @SuppressWarnings('SpaceBeforeOpeningBrace')
    private void logReadme(CreateCommandInfo createData) {
        // Find a suitable README and display that if it exists.
        def readmeFiles = createData.targetDir.listFiles({ File dir, String name ->
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