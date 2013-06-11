package uk.co.cacoethes.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Rule
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import uk.co.cacoethes.gradle.util.NameConverter

/**
 * A rule that creates tasks for installing a Lazybones template package into the
 * template cache (typically ~/.lazybones/templates). The tasks have the name
 * 'installTemplate<templateName>', where the template name is in camel-case. The
 * install tasks are automatically configured to depend on the corresponding
 * package task defined by {@link PackageTemplateRule}.
 */
class InstallTemplateRule implements Rule {
    Project project

    InstallTemplateRule(Project project) {
        this.project = project
    }

    @Override
    void apply(String taskName) {
        def m = taskName =~ /installTemplate([A-Z]\S+)/
        if (m) {
            def camelCaseTmplName = m[0][1]

            def pkgTask = (Zip) project.tasks.getByName("packageTemplate${camelCaseTmplName}")
            if (!pkgTask) return

            project.tasks.create(taskName, Copy).with {
                from pkgTask
                rename { String fileName -> fileName.replace(project.extensions.lazybones.packageNameSuffix, '') }
                into project.extensions.lazybones.installDir
            }
        }
    }

    @Override
    String getDescription() {
        return "installTemplate<tmplName> - Installs the named template package into your local cache"
    }

    @Override
    String toString() { return "Rule: $description" }
}
