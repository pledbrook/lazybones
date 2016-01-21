package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 12/04/2014.
 */
interface TemplateInstaller {
    void installTemplate(File packageFile, File targetDir, Map model)
    void unpackTemplate(File packageFile, File targetDir)
    void runPostInstallScript(File targetDir, Map model)
    String getReadme(File targetDir)
}
