package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import joptsimple.OptionSet
import org.codehaus.groovy.control.CompilerConfiguration
import uk.co.cacoethes.lazybones.LazybonesMain
import uk.co.cacoethes.lazybones.LazybonesScript
import uk.co.cacoethes.lazybones.LazybonesScriptException
import uk.co.cacoethes.lazybones.scm.GitAdapter
import uk.co.cacoethes.lazybones.scm.ScmAdapter


@Log
class InstallationScriptExecuter {
    private ScmAdapter scmAdapter

    void runPostInstallScriptWithArgs(OptionSet cmdOptions, CreateCommandInfo createData) {

        if (cmdOptions.has(CreateCommand.GIT_OPT)) scmAdapter = new GitAdapter()

        // Run the post-install script if it exists. The user can pass variables
        // to the script via -P command line arguments. This also places
        // lazybonesVersion, lazybonesMajorVersion, and lazybonesMinorVersion
        // variables into the script binding.
        try {
            def scriptVariables = cmdOptions.valuesOf(CreateCommand.VAR_OPT).
                    collectEntries { String it -> it.split('=') as List }

            scriptVariables << evaluateVersionScriptVariables()
            runPostInstallScript(createData.targetDir, scriptVariables)
            initScmRepo(createData.targetDir.absoluteFile)
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
    Script runPostInstallScript(File targetDir, Map<String, String> model) {
        def file = new File(targetDir, "lazybones.groovy")
        if (file.exists()) {
            def compiler = new CompilerConfiguration()
            compiler.scriptBaseClass = LazybonesScript.name

            // Can't use 'this' here because the static type checker does not
            // treat it as the class instance:
            //       https://jira.codehaus.org/browse/GROOVY-6162
            def shell = new GroovyShell(getClass().classLoader, new Binding(model), compiler)

            LazybonesScript script = shell.parse(file) as LazybonesScript
            script.setTargetDir(targetDir.path)
            script.scmExclusionFile = scmAdapter ? new File(targetDir, scmAdapter.exclusionsFilename) : null
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

    private void initScmRepo(File location) {
        if (scmAdapter) {
            scmAdapter.initializeRepository(location)
            scmAdapter.commitInitialFiles(location, "Initial commit")
        }
    }
}
