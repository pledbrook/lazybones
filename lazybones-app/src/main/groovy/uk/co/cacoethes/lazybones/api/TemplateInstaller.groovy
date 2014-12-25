package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 12/04/2014.
 */
interface TemplateInstaller {
    NewProjectInfo installTemplate(File packageFile, File targetDir, List<String> tmplQualifiers, Map model)
    NewProjectInfo installSubtemplate(String name, File projectDir, List<String> tmplQualifiers, Map model)
    NewProjectInfo unpackTemplate(File packageFile, File targetDir)

    /**
     *
     * @param projectDir
     * @param templateDir
     * @param tmplQualifiers
     * @param model
     */
    void runPostInstallScript(File projectDir, File templateDir, List<String> tmplQualifiers, Map model)
}
