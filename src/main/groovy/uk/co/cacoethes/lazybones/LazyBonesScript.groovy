package uk.co.cacoethes.lazybones

import uk.co.cacoethes.util.FilterMethods

import javax.naming.OperationNotSupportedException
import java.lang.reflect.Method
import groovy.util.AntBuilder

/**
 * Base script that will be applied to th lazybones.groovy root script in a lazybones template
 *
 * @author Tommy Barker
 */
class LazyBonesScript extends Script {

    protected static final String DEFAULT_ENCODING = "utf-8"
    final Map options = [:]

    String targetDir
    String encoding

    String getEncoding() {
        encoding ?: DEFAULT_ENCODING
    }

    /**
     *
     * user can ask for variables to be filled, if no input is provided the defaultValue is returned (null if not set).
     *
     * @param message
     * @param optionName optional, if the data already exists in the options map, it is returned intead
     * @param defaultValue optional, used if the user provides no input
     *
     * @return the option if it already exists, the next line from the user, or the default value if the user did not input anything.
     *
     */
    def ask(String message, String optionName = null, defaultValue = null) {
        if(optionName) {
            if(options.containsKey(optionName)) {
                return options[optionName]
            }
        }

        String response
        System.out.println message
        System.in.withReader {Reader reader ->
            response = reader.readLine()
        }

        response ?: defaultValue
    }

    def filterFiles(String filePattern, Map substitutionVariables) {
        def scanner = FilterMethods.getFiles(targetDir, filePattern)
        FilterMethods.filterFiles(scanner, encoding, substitutionVariables)
        return this
    }

    /**
     * uses the options / cli variables to filter files.  filePattern is usd by the ant <code>fileScanner</code>.
     * See {@link http://groovy.codehaus.org/Using+Ant+from+Groovy} or more details.
     *
     * @param filePattern
     */
    def filterFiles(String filePattern) {
        filterFiles(filePattern, options)
        return this
    }

    @Override
    Object run() {
        throw new UnsupportedOperationException("${this.getClass().name} is meant to be used directly, instead it shouldbe used as a base script")
    }

    boolean hasFeature(String featureName) {
        return this.getClass().methods.any {Method method -> method.name == featureName }
    }
}
