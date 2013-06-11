package uk.co.cacoethes.gradle.lazybones

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/**
 * The Lazybones Gradle conventions class. This contains the settings that users
 * can override in their own build scripts to customise the behaviour of the
 * tasks provided by the plugin.
 */
class LazybonesConventions {
    Project project

    /**
     * The directories containing the project's templates. The plugin will create
     * package, install and publish tasks for every directory in this collection.
     */
    FileCollection templateDirs

    /** The location where template packages are created by the build. */
    File packagesDir

    /** The location to install packaged templates into. */
    File installDir

    /**
     * The suffix appended to a template's name, forming the base name of the
     * template's package zip file.
     */
    String packageNameSuffix

    /**
     * The base Bintray URL to publish templates to. This should be the API URL
     * for publishing to a generic Bintray repository. For example:
     * https://api.bintray.com/content/pledbrook/lazybones-templates
     */
    String repositoryUrl

    /** The Bintray account to use when publishing the templates */
    String repositoryUsername

    /** The Bintray API key for the {@link repositoryUsername} account. */
    String repositoryApiKey

    LazybonesConventions(Project project) {
        this.project = project
    }

    /**
     * Adds one or more directories to the list of template directories that
     * the plugin will process.
     */
    def templateDirs(Object... dirs) {
        templateDirs.add(project.files(dirs))
    }
}
