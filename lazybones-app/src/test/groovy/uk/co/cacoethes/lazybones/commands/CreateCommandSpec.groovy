package uk.co.cacoethes.lazybones.commands

import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created by tbarker on 12/18/13.
 */
class CreateCommandSpec extends Specification {

    void "check mappings validation"() {
        given:
        def config = new ConfigObject()
        config.foo = "http://bar.com"

        when:
        CreateCommand.validateMappings(config)

        then:
        noExceptionThrown()

        when:
        config.bar = "notAUrl"
        CreateCommand.validateMappings(config)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "the value [notAUrl] for mapping [bar] is not a url"
    }

    void "package name is replaced with url"() {
        given:
        def config = new ConfigObject()
        config.templates.mappings.foo = "http://bar.com"
        config.cache.dir = new TemporaryFolder().newFolder()

        when:
        def createInfo = new CreateCommand(config).getCreateInfoFromArgs(["foo", "bar"])

        then:
        "http://bar.com" == createInfo.packageName
    }
}
