package uk.co.cacoethes.lazybones.commands

/**
 * The create and generate commands can accept qualified template names as
 * arguments. This class represents such names and allows easy access to the
 * core template name + the qualifiers.
 */
class TemplateArg {
    private final List<String> tmplParts

    TemplateArg(String arg) {
        tmplParts = arg.split(/::/) as List
    }

    String getTemplateName() { return tmplParts.head() }
    List<String> getQualifiers() { return tmplParts.tail() }
}
