package uk.co.cacoethes.util

import spock.lang.Specification

/**
 * Created by tbarker on 12/18/13.
 */
class UrlUtilsSpec extends Specification {

    void "test url validation"() {
        expect:
        a == UrlUtils.isUrl(b)

        where:
        a     | b
        true  | "http://foo.com"
        false | "foo.com"
        false | null
        false | ""
    }
}
