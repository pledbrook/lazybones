package uk.co.cacoethes.lazybones

import spock.lang.Specification
import uk.co.cacoethes.lazybones.config.Configuration

/**
 * @author Peter Ledbrook
 */
class LazybonesMainSpec extends Specification {

    void "Checking config with valid mapping URLs"() {
        given: "An initial configuration with valid mapping URLs"
        def config = initConfig(templates: [mappings: [foo: "http://bar.com"]])

        when:
        LazybonesMain.validateConfig(config)

        then:
        noExceptionThrown()
    }

    void "Checking config with invalid mapping URLs"() {
        given: "An initial configuration with an invalid mapping URL"
        def config = initConfig(templates: [mappings: [bar: "notAUrl"]])

        when: "I validate the config"
        LazybonesMain.validateConfig(config)

        then: "An exception is thrown"
        def exception = thrown(IllegalArgumentException)
        exception.message == "the value [notAUrl] for mapping [bar] is not a url"
    }

    protected Configuration initConfig(Map settings) {
        return new Configuration(
                new ConfigObject(),
                settings,
                [:],
                ["templates.mappings.*": URI],
                new File("delete-me.json"))
    }
}
