package uk.co.cacoethes.lazybones

import javax.naming.OperationNotSupportedException
import java.lang.reflect.Method

/**
 * Base script that will be applied to th lazybones.groovy root script in a lazybones template
 *
 * @author Tommy Barker
 */
@groovy.transform.CompileStatic
class LazyBonesScript extends Script {

    final Map options = [:]

    def ask(String message, String optionName, defaultValue) {
        throw new OperationNotSupportedException("ask is not supported yet")
    }

    def filterFiles(String filePattern, Map substitutionVariables) {
        throw new OperationNotSupportedException("fileterFiles is not supported yet")
    }

    def filterFiles(String filePattern) {
        filterFiles(filePattern, options)
    }

    @Override
    Object run() {
        throw new UnsupportedOperationException("${this.getClass().name} is meant to be used directly, instead it shouldbe used as a base script")
    }

    boolean hasFeature(String featureName) {
        return this.getClass().methods.any {Method method -> method.name == featureName }
    }
}
