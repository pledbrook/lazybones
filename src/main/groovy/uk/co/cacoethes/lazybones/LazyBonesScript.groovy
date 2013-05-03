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

    def ask(String message, String optionName, defaultValue) {
        throw new OperationNotSupportedException("ask is not supported yet")
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
