package uk.co.cacoethes.util

import spock.lang.Specification

/**
 * Created by tbarker on 12/18/13.
 */
class UrlUtilsSpec extends Specification {

    void "test url validation"() {
        expect:
        UrlUtils.isUrl("http://foo.com")
        !UrlUtils.isUrl("foo.com")
    }
}
