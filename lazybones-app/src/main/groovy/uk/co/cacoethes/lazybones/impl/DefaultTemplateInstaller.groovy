package uk.co.cacoethes.lazybones.impl

import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Log
import org.codehaus.groovy.control.CompilerConfiguration
import uk.co.cacoethes.lazybones.LazybonesScript
import uk.co.cacoethes.lazybones.api.TemplateInstaller
import uk.co.cacoethes.lazybones.scm.ScmAdapter
import uk.co.cacoethes.util.ArchiveMethods

/**
 * Created by pledbrook on 12/04/2014.
 */
@Log
class DefaultTemplateInstaller implements TemplateInstaller {
    private static final String README_BASENAME = "README"

    ScmAdapter scmAdapter

    @Override
    void createFromTemplate(File packageFile, File targetDir, Map model) {
        unpackTemplate(packageFile, targetDir)
        runPostInstallScript(targetDir, model)
    }

    @Override
    void unpackTemplate(File packageFile, File targetDir) {
        targetDir.mkdirs()
        ArchiveMethods.unzip(packageFile, targetDir)
    }

    @Override
    void runPostInstallScript(File targetDir, Map model) {
        def file = new File(targetDir, "lazybones.groovy")
        if (file.exists()) {
            GroovyShell shell = createShell(model)

            LazybonesScript script = shell.parse(file) as LazybonesScript
            initScript(script, targetDir, file).run()
            file.delete()
        }
    }

    @SuppressWarnings('SpaceBeforeOpeningBrace')
    String getReadme(File targetDir) {
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

    protected LazybonesScript initScript(LazybonesScript script, File targetDir, File file) {
        // Setter methods must be used here otherwise the physical properties on the
        // script object won't be set. I can only assume that the properties are added
        // to the script binding instead.
        def groovyEngine = new SimpleTemplateEngine()
        script.registerDefaultEngine(groovyEngine)
        script.registerEngine("gtpl", groovyEngine)
        script.setTargetDir(targetDir.path)
        script.setScmExclusionsFile(scmAdapter != null ? new File(targetDir, scmAdapter.exclusionsFilename) : null)
        return script
    }
}
