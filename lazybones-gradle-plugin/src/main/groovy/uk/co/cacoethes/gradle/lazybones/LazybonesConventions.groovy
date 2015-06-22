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

    /**
     * A list of convention objects, which allow further configuration of individual
     * templates.
     */
    List<TemplateConvention> templateConventions = []

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
     * A collection of Ant file patterns for files that should be excluded from
     * the template packages. For example, the VERSION file and temporary editor
     * files.
     */
    Collection<String> packageExcludes = []

    /**
     * A map of file modes to Ant-style patterns representing the files that
     * those file modes should apply to when packaging templates.
     */
    Map<String, String> fileModes = [:]

    /**
     * The base Bintray URL to publish templates to. This should be the API URL
     * for publishing to a generic Bintray repository. For example:
     * https://api.bintray.com/content/pledbrook/lazybones-templates
     */
    String repositoryUrl

    /**
     * Bintray repository name, in the form [user]/[repo]. Preferred over
     * {@link #repositoryUrl} as the base Bintray URL is already known.
     */
    String repositoryName

    /**
     * The licenses that the packages will be available under, such as Apache
     * 2.0 and GPL 3. Required for OSS packages.
     */
    List<String> licenses

    /**
     * The URL where the templates project is hosted. Required for OSS projects.
     */
    String vcsUrl

    /** The Bintray account to use when publishing the templates */
    String repositoryUsername

    /** The Bintray API key for the {@link #repositoryUsername} account. */
    String repositoryApiKey

    /**
     * Determines whether the templates will be published as soon as they
     * are uploaded. If this is {@code false}, you will need to publish the
     * artifact separately so that users can access it.
     */
    boolean publish

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

    /**
     * Adds one or more Ant file patterns to the package exclusions. Returns
     * the current exclusions.
     */
    Collection<String> packageExclude(String... patterns) {
        packageExcludes.addAll(patterns)
        return Collections.unmodifiableCollection(packageExcludes)
    }

    /**
     * Registers a set of Ant-style file path patterns against a given file
     * mode. This ensures that any files matching those patterns at package
     * time get the corresponding file permissions.
     * @param mode The Unix file mode representing file permissions.
     * @param patterns A collection of Ant-style path patterns.
     */
    void fileMode(String mode, String... patterns) {
        def modePatterns = fileModes[mode]
        if (modePatterns == null) fileModes[mode] = modePatterns = []
        modePatterns.addAll(patterns as List)
    }

    /**
     * Creates a new template convention for the template with the given name.
     * The convention object is added to {@link #templateConventions}.
     * @param name The name of the template to configure.
     * @return The new convention object, allowing for further configuration.
     */
    TemplateConvention template(String name, Closure c = null) {
        def convention = templateConventions.find { it.name == name }
        if (!convention) {
            convention = new TemplateConvention(name)
            templateConventions << convention
        }

        if (c) configureTemplate(c, convention)

        return convention
    }

    protected configureTemplate(Closure c, TemplateConvention convention) {
        def config = c.clone()
        config.delegate = convention
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config.call()
    }
}
