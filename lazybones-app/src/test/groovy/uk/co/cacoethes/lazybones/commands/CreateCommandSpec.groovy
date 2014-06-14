package uk.co.cacoethes.lazybones.commands

import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created by tbarker on 12/18/13.
 */
class CreateCommandSpec extends Specification {

    void "package name is replaced with url"() {
        given:
        def config = new ConfigObject()
        config.templates.mappings.foo = "http://bar.com"
        config.cache.dir = new TemporaryFolder().newFolder()

        when:
        def createInfo = new CreateCommand(config).getCreateInfoFromArgs(["foo", "bar"])

        then:
        "http://bar.com" == createInfo.packageArg.templateName
    }
}
