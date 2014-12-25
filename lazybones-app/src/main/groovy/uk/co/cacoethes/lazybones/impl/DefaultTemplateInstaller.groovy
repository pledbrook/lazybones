package uk.co.cacoethes.lazybones.impl

import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Log
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.control.CompilerConfiguration
import uk.co.cacoethes.lazybones.LazybonesScript
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.api.NewProjectInfo
import uk.co.cacoethes.lazybones.api.TemplateInstaller
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.lazybones.scm.ScmAdapter
import uk.co.cacoethes.util.ArchiveMethods

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by pledbrook on 12/04/2014.
 */
@Log
class DefaultTemplateInstaller implements TemplateInstaller {
    static final String SUBTEMPLATES_PARENT_PATH = ".lazybones"
    static final String STORED_PROPS_FILENAME = "stored-params.properties"
    static final String FILE_ENCODING = "UTF-8"

    private static final String README_BASENAME = "README"

    ScmAdapter scmAdapter

    @Override
    NewProjectInfo installTemplate(File packageFile, File targetDir, List<String> tmplQualifiers, Map model) {
        def templateInfo = unpackTemplate(packageFile, targetDir)
        runPostInstallScript(targetDir, targetDir, tmplQualifiers, model)

        // Only initialise an SCM repository after the post-install script has
        // run, otherwise we'll be adding file templates rather than the processed
        // files.
        initScmRepo(targetDir.absoluteFile)
        return templateInfo
    }

    @Override
    NewProjectInfo installSubtemplate(String name, File projectDir, List<String> tmplQualifiers, Map model) {
        // Make sure this is a Lazybones-created project, otherwise there are
        // no subtemplates to use.
        def unpackedSubtemplatesDir = new File(projectDir, SUBTEMPLATES_PARENT_PATH)
        if (!unpackedSubtemplatesDir.exists()) {
            def msg = "You cannot install subtemplates here as this is not a Lazybones-created project"
            log.severe msg
            throw new RuntimeException(msg)
        }

        def outDir = new File(unpackedSubtemplatesDir, "${name}-unpacked")
        try {
            def templateInfo = unpackTemplate(templateNameToPackageFile(unpackedSubtemplatesDir, name), outDir)
            runPostInstallScript(projectDir, outDir, tmplQualifiers, model)
            return templateInfo
        }
        finally {
            FileUtils.deleteDirectory(outDir)
        }
    }

    @Override
    NewProjectInfo unpackTemplate(File packageFile, File targetDir) {
        targetDir.mkdirs()
        ArchiveMethods.unzip(packageFile, targetDir)
        return new NewProjectInfo(targetDir, getReadme(targetDir))
    }

    @Override
    void runPostInstallScript(File projectDir, File templateDir, List<String> tmplQualifiers, Map model) {
        def installScriptFile = new File(templateDir, "lazybones.groovy")
        if (installScriptFile.exists()) {
            def scriptVariables = new HashMap(model)
            scriptVariables << loadParentParams(templateDir)
            scriptVariables << evaluateVersionScriptVariables()

            def script = initScript(installScriptFile, projectDir, templateDir, tmplQualifiers, model)
            script.run()

            installScriptFile.delete()
            persistParentParams(templateDir, script)
        }
    }

    @SuppressWarnings("SpaceBeforeOpeningBrace")
    protected File templateNameToPackageFile(File subtemplatesDir, String name) {
        def matchingFiles = subtemplatesDir.listFiles({ File f ->
            f.name ==~ /^${name}\-template\-.*\.zip$/
        } as FileFilter)

        if (!matchingFiles) throw new PackageNotFoundException("Cannot find a subtemplate named '$name'")
        return matchingFiles[0]
    }

    @SuppressWarnings('SpaceBeforeOpeningBrace')
    protected String getReadme(File targetDir) {
        def readmeFiles = targetDir.listFiles({ File dir, String name ->
            name == README_BASENAME || name.startsWith(README_BASENAME)
        } as FilenameFilter)

        return readmeFiles ? readmeFiles[0].text : null
    }

    protected GroovyShell createShell(Map model) {
        def compiler = new CompilerConfiguration()
        compiler.scriptBaseClass = LazybonesScript.name

        return new GroovyShell(getClass().classLoader, new Binding(model), compiler)
    }

    @SuppressWarnings("UnnecessaryGetter")
    protected void persistParentParams(File dir, LazybonesScript script) {
        // Save this template's named parameters in a file inside a .lazybones
        // sub-directory of the unpacked template.
        def lzbDir = new File(dir, ".lazybones")
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
        def lzbDir = templateDir.parentFile
        if (!lzbDir) return [:]

        def paramsFile = new File(lzbDir, STORED_PROPS_FILENAME)
        def props = new Properties()
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
        def version = Configuration.readVersion()
        def vars = [lazybonesVersion: version]

        def versionParts = version.split(/[\.\-]/)
        assert versionParts.size() > 1

        vars["lazybonesMajorVersion"] = versionParts[0]?.toInteger()
        vars["lazybonesMinorVersion"] = versionParts[1]?.toInteger()

        return vars
    }

    protected LazybonesScript initScript(
            File scriptFile,
            File projectDir,
            File templateDir,
            List<String> tmplQualifiers,
            Map<String, String> model) {
        def compiler = new CompilerConfiguration()
        compiler.scriptBaseClass = LazybonesScript.name

        // Can't use 'this' here because the static type checker does not
        // treat it as the class instance:
        //       https://jira.codehaus.org/browse/GROOVY-6162
        def shell = new GroovyShell(getClass().classLoader, new Binding(model), compiler)

        // Setter methods must be used here otherwise the physical properties on the
        // script object won't be set. I can only assume that the properties are added
        // to the script binding instead.
        LazybonesScript script = shell.parse(scriptFile) as LazybonesScript
        def groovyEngine = new SimpleTemplateEngine()
        script.with {
            registerDefaultEngine(groovyEngine)
            registerEngine("gtpl", groovyEngine)
            setTmplQualifiers(tmplQualifiers)
            setProjectDir(projectDir)
            setTemplateDir(templateDir)
            setScmExclusionsFile(scmAdapter != null ? new File(projectDir, scmAdapter.exclusionsFilename) : null)
        }
        return script
    }

    protected void initScmRepo(File location) {
        if (scmAdapter) {
            scmAdapter.initializeRepository(location)
            scmAdapter.commitInitialFiles(location, "Initial commit")
        }
    }
}
