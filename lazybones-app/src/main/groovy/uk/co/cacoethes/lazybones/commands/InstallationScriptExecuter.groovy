package uk.co.cacoethes.lazybones.commands

import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Log
import org.codehaus.groovy.control.CompilerConfiguration
import uk.co.cacoethes.lazybones.LazybonesMain
import uk.co.cacoethes.lazybones.LazybonesScript
import uk.co.cacoethes.lazybones.LazybonesScriptException
import uk.co.cacoethes.lazybones.scm.ScmAdapter

/**
 * Sets up and runs a post-install script, managing properties provided by
 * parent templates, SCM integration, and setting the appropriate script base
 * class.
 */
@Log
class InstallationScriptExecuter {
    static final String STORED_PROPS_FILENAME = "stored-params.properties"
    static final String FILE_ENCODING = "UTF-8"

    private ScmAdapter scmAdapter

    InstallationScriptExecuter() {
        this(null)
    }

    InstallationScriptExecuter(ScmAdapter adapter) {
        this.scmAdapter = adapter
    }

    @SuppressWarnings("ParameterReassignment")
    void runPostInstallScriptWithArgs(
            Map variables,
            List tmplQualifiers,
            File targetDir,
            File templateDir = null) {
        templateDir = templateDir ?: targetDir

        // Run the post-install script if it exists. The user can pass variables
        // to the script via -P command line arguments. This also places
        // lazybonesVersion, lazybonesMajorVersion, and lazybonesMinorVersion
        // variables into the script binding.
        try {
            Map scriptVariables = new HashMap(variables)
            scriptVariables << loadParentParams(templateDir)
            scriptVariables << evaluateVersionScriptVariables()
            runPostInstallScript(tmplQualifiers, targetDir, templateDir, scriptVariables)
            initScmRepo(targetDir.absoluteFile)
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
    Script runPostInstallScript(List tmplQualifiers, File targetDir, File templateDir, Map<String, String> model) {
        File installScriptFile = new File(templateDir, "lazybones.groovy")
        if (installScriptFile.exists()) {
            LazybonesScript script = initializeScript(model, tmplQualifiers, installScriptFile, targetDir, templateDir)
            script.run()
            installScriptFile.delete()

            persistParentParams(templateDir, script)
            return script
        }

        return null
    }

    protected LazybonesScript initializeScript(
            Map<String, String> model,
            List<String> tmplQualifiers,
            File scriptFile,
            File targetDir,
            File templateDir) {
        CompilerConfiguration compiler = new CompilerConfiguration()
        compiler.scriptBaseClass = LazybonesScript.name

        // Can't use 'this' here because the static type checker does not
        // treat it as the class instance:
        //       https://jira.codehaus.org/browse/GROOVY-6162
        GroovyShell shell = new GroovyShell(getClass().classLoader, new Binding(model), compiler)

        // Setter methods must be used here otherwise the physical properties on the
        // script object won't be set. I can only assume that the properties are added
        // to the script binding instead.
        LazybonesScript script = shell.parse(scriptFile) as LazybonesScript
        SimpleTemplateEngine groovyEngine = new SimpleTemplateEngine()
        script.with {
            registerDefaultEngine(groovyEngine)
            registerEngine("gtpl", groovyEngine)
            setTmplQualifiers(tmplQualifiers)
            setProjectDir(targetDir)
            setTemplateDir(templateDir)
            setScmExclusionsFile(scmAdapter != null ? new File(targetDir, scmAdapter.exclusionsFilename) : null)
        }
        return script
    }

    @SuppressWarnings("UnnecessaryGetter")
    protected void persistParentParams(File dir, LazybonesScript script) {
        // Save this template's named parameters in a file inside a .lazybones
        // sub-directory of the unpacked template.
        File lzbDir = new File(dir, ".lazybones")
        lzbDir.mkdirs()
        new File(lzbDir, STORED_PROPS_FILENAME).withWriter(FILE_ENCODING) { Writer w ->
            // Need to use the getter method explicitly, otherwise it seems to
            // return an empty map.
            (script.getParentParams() as Properties).store(w, "Lazybones saved template parameters")
        }
    }

    protected Map loadParentParams(File templateDir) {
        // Use the unpacked template's directory as the reference point and
        // then treat its parent directory as the location for the stored
        // parameters. If `tempateDir` is CWD, then the parent directory will
        // actually be null, in which case there is no store parameters file
        // (for example in the case of an unpacked project template rather
        // than a subtemplate).
        File lzbDir = templateDir.parentFile
        if (!lzbDir) return [:]

        File paramsFile = new File(lzbDir, STORED_PROPS_FILENAME)
        Properties props = new Properties()
        if (paramsFile.exists()) {
            paramsFile.withReader(FILE_ENCODING) { Reader r ->
                props.load(r)
            }
        }

        return [parentParams: props as Map]
    }

    /**
     * Reads the Lazybones version, breaks it up, and adds {@code lazybonesVersion},
     * {@code lazybonesMajorVersion}, and {@code lazybonesMinorVersion} variables
     * to a map that is then returned.
     */
    protected Map evaluateVersionScriptVariables() {
        String version = LazybonesMain.readVersion()
        Map vars = [lazybonesVersion: version]

        List versionParts = version.split(/[\.\-]/)
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
