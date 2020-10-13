package uk.co.cacoethes.gradle.tasks

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Rule
import org.gradle.api.Task
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
        def m = taskName =~ /packageTemplate([A-Z\-]\S+)/
        if (m) {
            def tmplName = taskToTemplateName(m[0][1])
            def tmplDir = project.extensions.lazybones.templateDirs.files.find { f -> f.name == tmplName }

            addSubTemplatesToPackageTask(
                    findTemplateConvention(tmplName),
                    createTask(taskName, tmplName, tmplDir))
        }
    }

    /**
     * Takes a template convention and uses it to configure the given package
     * task so that it both depends on all the necessary subtemplate packaging
     * tasks and also copies the subtemplate packages into the main template
     * package.
     */
    protected void addSubTemplatesToPackageTask(TemplateConvention tmplConvention, Task task) {
        if (!tmplConvention || !tmplConvention.includes) return

        def subPackageTasks = tmplConvention.includes.collect { String subpkgName ->
            project.tasks.getByName(subPackageTaskName(subpkgName))
        }

        task.dependsOn(subPackageTasks)
        task.from(subPackageTasks*.archivePath) {
            into(".lazybones")
            rename(/^subtmpl-(.*\.zip)/, '$1')
        }
    }

    /**
     * Finds the template convention for the given main template, if it exists.
     * The template convention allows for configuration of such things as what
     * subtemplates should be included in the project template.
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
        final tmplConvention = findTemplateConvention(tmplName)
        final packageExcludes = tmplConvention?.packageExcludes ?: project.extensions.lazybones.packageExcludes
        final fileModes = tmplConvention?.fileModes ?: project.extensions.lazybones.fileModes

        validateTemplateVersion tmplConvention, tmplDir, tmplName

        final Zip task = project.tasks.create(taskName, Zip) {
            archiveBaseName.set("${tmplName}${project.extensions.lazybones.packageNameSuffix}")
            destinationDirectory.set(project.extensions.lazybones.packagesDir)
            archiveVersion.set(tmplConvention?.version ?: project.file("$tmplDir/VERSION").text.trim())

            includeEmptyDirs = true
        }

        // Include directories and files that are not explicitly excluded and
        // don't have an explicit file mode associated with them.
        task.from tmplDir, {
            fileModes.each { String mode, List<String> patterns ->
                exclude patterns
            }

            // Can't use convention mapping here because `excludes` isn't a
            // real property (it delegates to a CopySpec object).
            if (packageExcludes) {
                exclude packageExcludes
            }
        }

        // Include directories and files that are configured with specific unix
        // permissions.
        if (fileModes) {
            fileModes.each { String mode, List<String> patterns ->
                task.from tmplDir, {
                    include patterns

                    // This is required otherwise excluded directories may
                    // actually be included.
                    if (packageExcludes) {
                        exclude packageExcludes
                    }

                    fileMode = unixModeStringToInteger(mode)
                    dirMode = unixModeStringToInteger(mode)
                }
            }
        }

        task.doFirst {
            validateTemplateDir(tmplDir, tmplName)
        }
        return task
    }

    protected void validateTemplateVersion(TemplateConvention tmplConvention, File tmplDir, String tmplName) {
        if (!tmplConvention?.version && !new File(tmplDir, "VERSION").exists()) {
            throw new InvalidUserDataException("Project template '${tmplName}' has no source of version info")
        }
    }

    protected String taskToTemplateName(String requestedTemplateName) {
        // The rule supports tasks of the form packageTemplateMyTmpl and
        // packageTemplate-my-tmpl. Only the former requires conversion of
        // the name to lowercase hyphenated.
        return requestedTemplateName.startsWith("-") ? requestedTemplateName.substring(1) :
                NameConverter.camelCaseToHyphenated(requestedTemplateName)
    }

    protected String subPackageTaskName(String subpkgName) {
        return "packageTemplateSubtmpl${NameConverter.hyphenatedToCamelCase(subpkgName)}".toString()
    }

    protected void validateTemplateDir(File tmplDir, String tmplName) {
        // Note that tmplDir *may* be null if the directory exists but isn't
        // included in the templateDirs extension property.
        if (!tmplDir?.exists()) {
            throw new InvalidUserDataException("No project template directory found for '${tmplName}'")
        }
    }

    protected unixModeStringToInteger(String mode) {
        return Integer.valueOf(mode, 8)
    }

    @Override
    String getDescription() {
        return "packageTemplate<TmplName> - Packages the template in the directory matching the task name"
    }

    @Override
    String toString() { return "Rule: $description" }
}
