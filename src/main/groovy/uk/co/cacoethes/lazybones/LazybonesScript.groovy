package uk.co.cacoethes.lazybones

import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Log

import java.lang.reflect.Method

/**
 * Base script that will be applied to the lazybones.groovy root script in a lazybones template
 *
 * @author Tommy Barker
 */
@Log
class LazybonesScript extends Script {

    protected static final String DEFAULT_ENCODING = "utf-8"

    /**
     * The root directory of the unpacked template. This is the base path used
     * when searching for files that need filtering.
     */
    String targetDir

    /**
     * The encoding/charset used by the files in the template. This is UTF-8
     * by default.
     */
    String fileEncoding = DEFAULT_ENCODING

    /**
     * The reader stream from which user input will be pulled. Defaults to a
     * wrapper around stdin using the platform's default encoding/charset.
     */
    Reader reader = new InputStreamReader(System.in)

    /**
     * Prints a message asking for a property value.  If the user has no response the default
     * value will be returned.  null can be returned
     *
     * @param message
     * @param defaultValue
     * @return the response
     */
    def ask(String message, defaultValue = null) {

        System.out.print message
        String line = reader.readLine()

        return line ?: defaultValue
    }

    /**
     * Prints a message asking for a property value.  If a value for the property already exists in
     * the binding of the script, it is used instead of asking the question.  If the user has no resopnse
     * the default value is returned
     *
     * @param message
     * @param propertyName name of the property to check for in the binding
     * @param defaultValue
     * @return the response
     */
    def ask(String message, String propertyName, defaultValue = null) {
        if (propertyName) {
            if (binding.hasVariable(propertyName)) {
                return binding.getVariable(propertyName)
            }
        }

        return ask(message, defaultValue)
    }

    def filterFiles(String filePattern, Map substitutionVariables) {
        if (!targetDir) {
            throw new IllegalStateException("targetDir has not been set")
        }
        def ant = new AntBuilder()
        def scanner = ant.fileScanner {
            fileset(dir: targetDir) {
                include(name: filePattern)
            }
        } as Iterable<File>
        boolean atLeastOneFileFiltered = filterFilesHelper(scanner, substitutionVariables)

        if (!atLeastOneFileFiltered) {
            log.warning "No files filtered with file pattern [$filePattern] and target directory [$targetDir]"
        }

        return this
    }

    /**
     *
     * @param files files to filter
     * @param properties properties used to filter files
     * @return true if at least one file was filtered
     */
    private boolean filterFilesHelper(Iterable<File> files, Map properties) {
        boolean atLeastOneFileFiltered = false

        //have to use for instead of each, closure causes issues when script is used as base script
        for (file in files) {
            filterFileHelper(file, properties)
            atLeastOneFileFiltered = true
        }

        return atLeastOneFileFiltered
    }

    private void filterFileHelper(File file, Map properties) {
        if (!file.exists()) {
            throw new IllegalArgumentException("file ${file} does not exist")
        }
        log.info "Filtering file $file"

        def engine = new SimpleTemplateEngine()
        def reader = file.newReader(fileEncoding)
        def template = engine.createTemplate(reader).make(properties)
        def out = new FileOutputStream(file)
        Writer writer = new OutputStreamWriter(out, fileEncoding)
        template.writeTo(writer)
    }

    @Override
    Object run() {
        throw new UnsupportedOperationException("${this.getClass().name} is not meant to be used directly. " +
                "It should instead be used as a base script")
    }

    /**
     * Determines whether the version of Lazybones loading the post-installation
     * script supports a particular feature. Current features include "ask" and
     * filterFiles for example.
     */
    boolean hasFeature(String featureName) {
        return this.getClass().methods.any { Method method -> method.name == featureName }
    }
}
