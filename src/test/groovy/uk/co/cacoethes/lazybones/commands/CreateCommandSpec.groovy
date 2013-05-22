package uk.co.cacoethes.lazybones.commands

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.co.cacoethes.lazybones.LazybonesMain
import uk.co.cacoethes.lazybones.LazybonesScript

/**
 * @author Tommy Barker
 */
class CreateCommandSpec extends Specification {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    void "lazybones gets deleted after running"() {
        given: "a create command instance"
        def cmd = new CreateCommand()

        when: "I run lazybones.groovy"
        File file = testFolder.newFile("lazybones.groovy")
        file.write("System.setProperty('ran','true')")
        cmd.runPostInstallScript(testFolder.root, [:])

        then: "the script is deleted"
        !file.exists()
        'true' == System.getProperty("ran")

        cleanup: "sets the system properties back the way they were"
        System.properties.remove("ran")
    }

    void "lazybones has the targetDir set before running"() {
        given: "a create command instance"
        def cmd = new CreateCommand()

        when: "when I run lazybones.groovy"
        File file = testFolder.newFile("lazybones.groovy")
        file.write("//do nothing")
        LazybonesScript script = cmd.runPostInstallScript(testFolder.root, [:])

        then: "the targetDir is set"
        assert script.getTargetDir()
    }

    void "if lazybones does not exist, nothing happens"() {
        given: "a create command instance"
        def cmd = new CreateCommand()

        when: "I runLazyBonesIfExists and file does not exist, nothing happens"
        File file = new File("foobar")

        then: "nothing happens"
        !file.exists()
        null == cmd.runPostInstallScript(testFolder.root, [:])
    }
}
