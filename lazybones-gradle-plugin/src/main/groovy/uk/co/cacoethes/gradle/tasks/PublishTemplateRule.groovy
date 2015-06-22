package uk.co.cacoethes.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Rule
import org.gradle.api.tasks.bundling.Zip

import uk.co.cacoethes.gradle.lazybones.LazybonesConventions

/**
 * <p>A rule that creates tasks for publishing a Lazybones template package into a
 * Bintray generic repository. The tasks have the name 'publishTemplate<templateName>',
 * where the template name is in camel-case. The publish tasks are automatically
 * configured to depend on the corresponding package task defined by
 * {@link PackageTemplateRule}.</p>
 * <p>The tasks also require the {@link LazybonesConventions#repositoryUrl}, {@link
 * LazybonesConventions#repositoryUsername} and {@link LazybonesConventions#repositoryApiKey}
 * properties to be set. If any are missing, the tasks will fail.</p>
 */
class PublishTemplateRule implements Rule {
    Project project

    PublishTemplateRule(Project project) {
        this.project = project
    }

    @Override
    void apply(String taskName) {
        def m = taskName =~ /publishTemplate([A-Z\-]\S+)/
        if (m) {
            def camelCaseTmplName = m[0][1]

            def pkgTask = (Zip) project.tasks.getByName("packageTemplate${camelCaseTmplName}")
            if (!pkgTask) return

            def lzbExtension = project.extensions.lazybones

            project.tasks.create(taskName, BintrayGenericUpload).with { t ->
                dependsOn pkgTask
                artifactFile = pkgTask.archivePath

                username = lzbExtension.repositoryUsername
                apiKey = lzbExtension.repositoryApiKey
                publish = lzbExtension.publish

                // Old style properties...
                artifactUrlPath = "${pkgTask.baseName}/${pkgTask.version}/${pkgTask.archiveName}"
                repositoryUrl = lzbExtension.repositoryUrl

                // ...superseded by these
                repositoryName = lzbExtension.repositoryName
                packageName = pkgTask.baseName
                licenses = lzbExtension.licenses
                vcsUrl = lzbExtension.vcsUrl

                doFirst {
                    def missingProps = verifyPublishProperties(t)
                    if (!repositoryUrl && !repositoryName) missingProps << "repositoryName"
                    if (missingProps) {
                        throw new GradleException(
                                """\
You must provide values for these settings:

    ${missingProps.join(", ")}

For example, in your build file:

    lazybones {
        repositoryName = "pledbrook/lazybones-templates"
        repositoryUsername = "pledbrook"
        repositoryApiKey = "KJFHEWJFJFJFHKSGHKH"
    }
""")
                    }

                    if (!artifactFile.exists()) {
                        throw new GradleException("Bad build file: zip archive '${pkgTask.archiveName}' does not exist," +
                                " but should have been created automatically.")
                    }
                }
            }
        }
    }

    /**
     * @return a list of convention properties that are required for publishing
     * to Bintray but don't have values set by the user.
     */
    protected List verifyPublishProperties(task) {
        ["username", "apiKey"].findAll { !task.getProperty(it) }
    }

    @Override
    String getDescription() {
        return "publishTemplate<TmplName> - Publishes the named template package to the configured Bintray repository"
    }

    @Override
    String toString() { return "Rule: $description" }
}
