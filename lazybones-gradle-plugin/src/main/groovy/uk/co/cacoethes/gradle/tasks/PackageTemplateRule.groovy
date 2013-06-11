package uk.co.cacoethes.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Rule
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.bundling.Zip
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
        def m = taskName =~ /packageTemplate([A-Z]\S+)/
        if (m) {
            def camelCaseTmplName = m[0][1]
            def tmplName = NameConverter.camelCaseToHyphenated(camelCaseTmplName)
            def tmplDir = project.extensions.lazybones.templateDirs.files.find { f -> f.name == tmplName }

            if (!tmplDir?.exists()) {
                project.logger.error "No project template directory found for '${tmplName}'"
                return
            }

            if (!new File(tmplDir, "VERSION").exists()) {
                project.logger.error "Project template '${tmplName}' has no VERSION file"
                return
            }

            project.tasks.create(taskName, Zip).with {
                conventionMapping.map("baseName") { tmplName + project.extensions.lazybones.packageNameSuffix }
                conventionMapping.map("destinationDir") { project.extensions.lazybones.packagesDir }

                excludes = ["**/.retain"]
                includeEmptyDirs = true
                version = project.file("$tmplDir/VERSION").text.trim()

                from tmplDir
                exclude "**/.retain", "VERSION", ".gradle"
            }
        }
    }

    @Override
    String getDescription() {
        return "packageTemplate<tmplName> - Packages the template in the directory matching the task name"
    }

    @Override
    String toString() { return "Rule: $description" }
}
