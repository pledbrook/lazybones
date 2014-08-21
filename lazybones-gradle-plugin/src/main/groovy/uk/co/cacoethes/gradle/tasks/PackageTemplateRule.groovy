package uk.co.cacoethes.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Rule
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.bundling.Zip
import uk.co.cacoethes.gradle.lazybones.TemplateConvention
import uk.co.cacoethes.gradle.util.NameConverter

/**
 * <p>A rule that creates tasks for packaging a Lazybones template as a zip file.
 * The tasks have the name 'packageTemplate<templateName>', where the template
 * name should be camel-case. The template name is then converted to hyphenated,
 * lower-case form to determine the directory containing the template source.</p>
 * <p>Each task is of type Zip and can be minimally configured through the {@link
 * uk.co.cacoethes.gradle.lazybones.LazybonesConventions Lazybones conventions}.
 * You can control the name suffix used for the name of the zip package (via
 * {@code packageNameSuffix} and where the packages are created (via {@code
 * packagesDir}. Also, the tasks automatically exclude .retain and VERSION files
 * from the generated zip file, as well as any .gradle directory in the root.</p>
 */
class PackageTemplateRule implements Rule {
    Project project

    PackageTemplateRule(Project project) {
        this.project = project
    }

    @Override
    void apply(String taskName) {
        def m = taskName =~ /packageTemplate([A-Z-]\S+)/
        if (m) {
            def camelCaseTmplName = m[0][1]
            def tmplName = camelCaseTmplName.startsWith("-") ? camelCaseTmplName.substring(1) :
                    NameConverter.camelCaseToHyphenated(camelCaseTmplName)
            def tmplDir = project.extensions.lazybones.templateDirs.files.find { f -> f.name == tmplName }

            if (!tmplDir?.exists()) {
                project.logger.error "No project template directory found for '${tmplName}'"
                return
            }

            if (!new File(tmplDir, "VERSION").exists()) {
                project.logger.error "Project template '${tmplName}' has no VERSION file"
                return
            }

            addSubTemplatesToPackageTask(
                    findTemplateConvention(tmplName),
                    createTask(taskName, tmplName, tmplDir))
        }
    }

    /**
     * Takes a template convention and uses it to configure the given package
     * task so that it both depends on all the necessary sub-template packaging
     * tasks and also copies the sub-template packages into the main template
     * package.
     */
    protected void addSubTemplatesToPackageTask(TemplateConvention tmplConvention, Task task) {
        if (!tmplConvention) return

        def subPackageTaskNames = tmplConvention.includes.collect { String subpkgName ->
            "packageTemplateSubtmpl${NameConverter.hyphenatedToCamelCase(subpkgName)}".toString()
        }

        task.dependsOn(subPackageTaskNames)
        task.into(".lazybones") {
            from subPackageTaskNames.collect { project.tasks.getByName(it) }*.archivePath
            rename(/^subtmpl-(.*\.zip)/, '$1')
        }
    }

    /**
     * Finds the template convention for the given main template, if it exists.
     * @param tmplName The name of the main template whose convention you want.
     * @return The convention object, or <code>null</code> if no template
     * convention was defined for the given template.
     */
    protected TemplateConvention findTemplateConvention(String tmplName) {
        return project.extensions.lazybones.templateConventions.find { it.name == tmplName }
    }

    /**
     * Creates the basic task for packaging a template as a zip file. In fact
     * the task itself is of type org.gradle.api.tasks.bundling.Zip.
     */
    protected Task createTask(String taskName, String tmplName, File tmplDir) {
        def task = project.tasks.create(taskName, Zip)
        task.with {
            conventionMapping.map("baseName") { tmplName + project.extensions.lazybones.packageNameSuffix }
            conventionMapping.map("destinationDir") { project.extensions.lazybones.packagesDir }

            includeEmptyDirs = true
            version = project.file("$tmplDir/VERSION").text.trim()

            from tmplDir

            // Can't use convention mapping here because `excludes` isn't a
            // real property (it delegates to a CopySpec object).
            excludes = project.extensions.lazybones.packageExcludes
        }
        return task
    }

    @Override
    String getDescription() {
        return "packageTemplate<TmplName> - Packages the template in the directory matching the task name"
    }

    @Override
    String toString() { return "Rule: $description" }
}
