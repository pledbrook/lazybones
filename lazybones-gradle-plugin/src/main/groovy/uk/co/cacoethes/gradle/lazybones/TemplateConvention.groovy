package uk.co.cacoethes.gradle.lazybones

/**
 * Used by {@link LazybonesConventions} to manage individual template settings,
 * such as what sub-templates are included in them.
 */
class TemplateConvention {
    private String name
    private List<String> subTemplates = []

    TemplateConvention(String name) {
        this.name = name
    }

    String getName() { return name }
    List<String> getIncludes() { return new ArrayList(subTemplates) }

    /**
     * Adds a set of sub-templates that will be included in this template's
     * package. Duplicates are automatically removed.
     * @param children The names of the sub-templates to include in the package.
     * These names should not include the sub-template prefix, by default 'subtmpl'.
     * @return This convention object.
     */
    TemplateConvention includes(String... children) {
        subTemplates.addAll(children)
        subTemplates.unique()
        return this
    }
}
