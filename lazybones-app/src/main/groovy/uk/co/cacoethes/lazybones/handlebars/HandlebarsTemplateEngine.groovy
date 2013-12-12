package uk.co.cacoethes.lazybones.handlebars

import com.github.jknack.handlebars.Handlebars
import groovy.text.Template
import groovy.text.TemplateEngine
import org.codehaus.groovy.control.CompilationFailedException

/**
 * Implementation of {@code groovy.text.TemplateEngine} for Handlebars
 *
 * @author andyjduncan
 */
class HandlebarsTemplateEngine extends TemplateEngine {

    def handlebars = new Handlebars()

    @Override
    Template createTemplate(Reader reader) throws CompilationFailedException, ClassNotFoundException, IOException {
        def nativeTemplate = handlebars.compileInline(reader.text)
        new HandlebarsTemplate(nativeTemplate)
    }
}
