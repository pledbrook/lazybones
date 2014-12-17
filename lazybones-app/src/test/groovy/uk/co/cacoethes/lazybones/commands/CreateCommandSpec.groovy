package uk.co.cacoethes.lazybones.commands

import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.co.cacoethes.lazybones.config.Configuration

/**
 * Created by tbarker on 12/18/13.
 */
class CreateCommandSpec extends Specification {

    void "package name is replaced with url"() {
        given:
        def config = initConfig(
                templates: [mappings: [foo: "http://bar.com"]],
                cache: [dir: new TemporaryFolder().newFolder().absolutePath])

        when:
        def createInfo = new CreateCommand(config).getCreateInfoFromArgs(["foo", "bar"])

        then:
        "http://bar.com" == createInfo.packageArg.templateName
    }

    protected Configuration initConfig(Map settings) {
        return new Configuration(
                new ConfigObject(),
                settings,
                [:],
                ["templates.mappings.*": URI,
                 "cache.dir": String],
                new File("delete-me.json"))
    }
}
