package uk.co.cacoethes.lazybones

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @author Tommy Barker
 */
class LazybonesMainSpec extends Specification {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    void "lazybones gets deleted after running"() {
        when: "I run lazybones.groovy"
        File file = testFolder.newFile("lazybones.groovy")
        file.write("System.setProperty('ran','true')")
        LazybonesMain.runPostInstallScript(testFolder.root)

        then: "the script is deleted"
        !file.exists()
        'true' == System.getProperty("ran")

        cleanup: "sets the system properties back the way they were"
        System.properties.remove("ran")
    }

    void "lazybones has the targetDir set before running"() {
        when: "when I run lazybones.groovy"
        File file = testFolder.newFile("lazybones.groovy")
        file.write("//do nothing")
        LazybonesScript script = LazybonesMain.runPostInstallScript(testFolder.root)

        then: "the targetDir is set"
        assert script.getTargetDir()
    }

    void "if lazybones does not exist, nothing happens"() {
        when: "I runLazyBonesIfExists and file does not exist, nothing happens"
        File file = new File("foobar")

        then: "nothing happens"
        !file.exists()
        null == LazybonesMain.runPostInstallScript(testFolder.root)
    }
}
