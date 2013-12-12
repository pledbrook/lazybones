package uk.co.cacoethes.lazybones.handlebars

import groovy.text.Template
import spock.lang.Specification

class HandlebarsTemplateSpec extends Specification {

    Template handlebarsTemplate

    Map binding

    def templated

    void "renders a empty template"() {
        when:
        apply_template ''

        then:
        got_templated ''
    }

    void "renders a simple text template"() {
        when:
        apply_template 'Hello'

        then:
        got_templated 'Hello'
    }

    void "applies a binding to a template"() {
        when:
        apply_template 'Hello {{this.message}}' with_binding message:'there'

        then:
        got_templated 'Hello there'
    }

    def apply_template(template) {
        def templateReader = new StringReader(template)
        def engine = new HandlebarsTemplateEngine()
        handlebarsTemplate = engine.createTemplate(templateReader)
        this
    }

    void with_binding(Map binding) {
        this.binding = binding
    }

    void got_templated(expectation) {
        def writeDestination = new StringWriter()
        if (binding) {
            handlebarsTemplate.make(binding).writeTo(writeDestination)
        } else {
            handlebarsTemplate.make().writeTo(writeDestination)
        }
        templated = writeDestination.toString()
        assert templated == expectation
    }
}