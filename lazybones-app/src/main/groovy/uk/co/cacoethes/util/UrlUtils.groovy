package uk.co.cacoethes.util

import groovy.transform.CompileStatic

/**
 * Created by tbarker on 12/18/13.
 */
@CompileStatic
class UrlUtils {

    /**
     * Determines whether the given package name is in fact a full blown URI,
     * including scheme.
     */
    static boolean isUrl(String str) {
        if (!str) return false
        try {
            def uri = new URI(str)
            return uri.scheme
        }
        catch (URISyntaxException ignored) {
            return false
        }
    }
}
