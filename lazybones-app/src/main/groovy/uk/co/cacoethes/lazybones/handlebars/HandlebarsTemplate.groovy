package uk.co.cacoethes.lazybones.handlebars

import groovy.text.Template

class HandlebarsTemplate implements Template {

    private final nativeTemplate

    HandlebarsTemplate(final com.github.jknack.handlebars.Template nativeTemplate) {
        this.nativeTemplate = nativeTemplate
    }

    @Override
    Writable make() {
        make null
    }

    @Override
    Writable make(final Map binding) {
        { Writer destination ->
            nativeTemplate.apply(binding, destination)
            destination.flush()
            destination
        } as Writable
    }
}
