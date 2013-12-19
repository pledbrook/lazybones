package uk.co.cacoethes.lazybones

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Peter Ledbrook
 */
class LazybonesMainSpec extends Specification {
    @Unroll
    def "Conversion of dotted string '#input' to config option"() {
        given: "A Groovy config object"
        def config = new ConfigObject()

        when: "A dotted string is applied to a config object with a new value"
        def result = LazybonesMain.setConfigOption(config, input, value)

        then: "That configuration option is set"
        result.containsKey(lastKey)
        result."${lastKey}" == value
        config.flatten().get(input) == value

        where:
        input                    |    value      |      lastKey
        ""                       |   "Hello"     |        ""
        "cacheDir"               |     null      |     "cacheDir"
        "cacheDir"               |    "Test"     |     "cacheDir"
        "config.file"            |  "/path/to"   |       "file"
        "lazybones.cache.dir"    |    "Test"     |       "dir"
    }

    void "check mappings validation"() {
        given:
        def config = new ConfigObject()
        config.templates.mappings.foo = "http://bar.com"

        when:
        LazybonesMain.validateConfig(config)

        then:
        noExceptionThrown()

        when:
        config.templates.mappings.bar = "notAUrl"
        LazybonesMain.validateConfig(config)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "the value [notAUrl] for mapping [bar] is not a url"
    }
}
