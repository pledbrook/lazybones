package uk.co.cacoethes.gradle.lazybones

/**
 * Used by {@link LazybonesConventions} to manage individual template settings,
 * such as what subtemplates are included in them.
 */
class TemplateConvention {
    private String name
    private List<String> subTemplates = []

    /**
     * The version string for this template.
     * @since 1.2.2
     */
    String version

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

    TemplateConvention(String name) {
        this.name = name
    }

    String getName() { return name }
    List<String> getIncludes() { return new ArrayList(subTemplates) }

    /**
     * Adds a set of subtemplates that will be included in this template's
     * package. Duplicates are automatically removed.
     * @param children The names of the subtemplates to include in the package.
     * These names should not include the subtemplate prefix, by default 'subtmpl'.
     * @return This convention object.
     */
    TemplateConvention includes(String... children) {
        subTemplates.addAll(children)
        subTemplates.unique()
        return this
    }

    /**
     * Adds one or more Ant file patterns to the package exclusions. Returns
     * the current exclusions.
     * @since 1.1
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
     * @since 1.2
     */
    void fileMode(String mode, String... patterns) {
        def modePatterns = fileModes[mode]
        if (modePatterns == null) fileModes[mode] = modePatterns = []
        modePatterns.addAll(patterns as List)
    }
}
