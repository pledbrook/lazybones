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

        // Shared configuration closure that can easily turn a standard task
        // into an aggregate of others based on the plugin's conventions.
        def addTaskDependencies = { String baseTaskName, Task task ->
            task.dependsOn {
                project.extensions.lazybones.templateDirs.filter { File f ->
                    !f.name.startsWith(SUBTEMPLATE_PREFIX)                    // Exclude sub-templates
                }.files.collect { File f ->
                    def camelCaseTmplName = NameConverter.hyphenatedToCamelCase(f.name)
                    project.tasks.getByName(baseTaskName + camelCaseTmplName)
                }
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
}
