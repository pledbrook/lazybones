package uk.co.cacoethes.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import uk.co.cacoethes.gradle.lazybones.LazybonesConventions
import uk.co.cacoethes.gradle.tasks.InstallTemplateRule
import uk.co.cacoethes.gradle.tasks.PackageTemplateRule
import uk.co.cacoethes.gradle.tasks.PublishTemplateRule
import uk.co.cacoethes.gradle.util.NameConverter

/**
 * Plugin to aid the packaging and distribution of Lazybones templates. It
 * provides both tasks and a [@link LazybonesConventions set of conventions}.
 * The tasks allow you to package the templates, install them into the
 * Lazybones cache and publish them to a Bintray generic repository.
 */
class LazybonesTemplatesPlugin implements Plugin<Project> {
    static final String SUBTEMPLATE_PREFIX = "subtmpl-"

    void apply(Project project) {
        project.apply plugin: "base"

        def defaultTemplateDirs = project.file("templates").listFiles({ it.isDirectory() } as FileFilter)

        // Create the plugin's conventions as an extension and set up the
        // default values for it.
        def extension = project.extensions.create("lazybones", LazybonesConventions, project)
        extension.templateDirs = defaultTemplateDirs ? project.files(defaultTemplateDirs) : project.files()
        extension.packagesDir = project.file("${project.buildDir}/packages")
        extension.installDir = new File(System.getProperty("user.home"), ".lazybones/templates")
        extension.packageNameSuffix = "-template"
        extension.packageExcludes = ["**/.retain", "VERSION", ".gradle"]
        extension.publish = false

        project.gradle.taskGraph.whenReady {
            project.extensions.lazybones.templateDirs.filter { File f ->
                !verifyTemplateDirName(f.name)
            }.files.each { File f ->
                project.logger.warn "WARN Template directory '${f.name} does not satisfy the convention " +
                        "of being lowercase hyphenated, so it will be ignored by packageAllTemplates et al " +
                        "by default. You can manually attach the packageTemplate-${f.name} task to " +
                        "packageAllTemplates via an explicit dependsOn in your build file."
            }
        }

        // Shared configuration closure that can easily turn a standard task
        // into an aggregate of others based on the plugin's conventions.
        def addTaskDependencies = { String baseTaskName, Task task ->
            task.dependsOn {
                project.extensions.lazybones.templateDirs.filter { File f ->
                    !f.name.startsWith(SUBTEMPLATE_PREFIX) &&     // Exclude subtemplates
                            verifyTemplateDirName(f.name)        // and those whose names are not lowercase hyphenated
                }.files.collect { File f ->
                    def camelCaseTmplName = NameConverter.hyphenatedToCamelCase(f.name)
                    project.tasks.getByName(baseTaskName + camelCaseTmplName)
                }.findAll { it != null }
            }
        }

        addPackageTasks project, addTaskDependencies.curry("packageTemplate")
        addInstallTasks project, addTaskDependencies.curry("installTemplate")
        addPublishTasks project, addTaskDependencies.curry("publishTemplate")
    }

    protected void addPackageTasks(Project project, Closure taskConfigurer) {
        project.tasks.addRule(new PackageTemplateRule(project))
        project.tasks.create("packageAllTemplates", taskConfigurer)
    }

    protected void addInstallTasks(Project project, Closure taskConfigurer) {
        project.tasks.addRule(new InstallTemplateRule(project))
        project.tasks.create("installAllTemplates", taskConfigurer)
    }

    protected void addPublishTasks(Project project, Closure taskConfigurer) {
        project.tasks.addRule(new PublishTemplateRule(project))
        project.tasks.create("publishAllTemplates", taskConfigurer)
    }

    /**
     * Checks that a template directory name is fully reversible according to
     * the plugin's naming conventions. In other words, it should be possible
     * to convert the name from lowercase hyphenated to camelcase and back
     * again, without losing information.
     * @param dirName The directory name to verify.
     * @return {@code true} if the name is reversible and hence supports the
     * plugin's naming convention.
     */
    protected boolean verifyTemplateDirName(String dirName) {
        return NameConverter.camelCaseToHyphenated(NameConverter.hyphenatedToCamelCase(dirName)) == dirName
    }
}
