package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.Rule

class LazybonesScriptFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        def proxyAddress = recorder.proxy.address()
        env["JAVA_OPTS"] = "-Dhttps.proxyHost=" + proxyAddress.hostName + " -Dhttps.proxyPort=" + proxyAddress.port
    }

    @Betamax(tape="create-tape")
    def "lazybones is deleted after package is installed"() {
        given: "The Lazybones version"
        def lazybonesVersion = readLazybonesVersion()


        when: "I run lazybones with the create command for the groovy-gradle template"
        def exitCode = runCommand(["create", "test-tmpl", "0.2", "groovyapp"], baseWorkDir, ["foo", "0.1"])

        then: "It successfully completes, deleting the lazybones script after unpacking the template"
        exitCode == 0
        def appDir = new File(baseWorkDir, "groovyapp")
        !new File(appDir, "lazybones.groovy").exists()

        and: "It runs the post install script, substituting in the given group and version into the build file"
        def buildGradleText = new File(appDir, "build.gradle").text
        buildGradleText.contains("group = \"foo\"")
        buildGradleText.contains("version = \"0.1\"")

        and: "Files are properly filtered"
        def printHelloText = new File(appDir, "src/main/groovy/PrintHello.groovy").text
        printHelloText.contains("foo")
        printHelloText.contains('"${bar}')
        def printGoodbyeText = new File(appDir, "src/main/groovy/PrintGoodbye.groovy").text
        printGoodbyeText.contains("foo")
        def unfilteredText = new File(appDir, "src/main/groovy/UnfilteredHello.groovy").text
        unfilteredText.contains('${name}')

        and: "the post-install script creates a text file containing the version number"
        def testText = new File(appDir, "test.txt").text
        testText.contains("Your Lazybones version is OK - you're good to go!")
        !testText.contains("Your Lazybones version is too old")
        testText.contains("Version: ${lazybonesVersion}")
    }

    @Betamax(tape="create-tape")
    def "Default 'ask' values are used"() {
        when: "I run lazybones with the create command for the groovy-gradle template"
        def exitCode = runCommand(["create", "test-tmpl", "0.2", "groovyapp"], baseWorkDir, ["foo", ""])

        then: "It successfully completes, deleting the lazybones script after unpacking the template"
        exitCode == 0
        def appDir = new File(baseWorkDir, "groovyapp")
        !new File(appDir, "lazybones.groovy").exists()

        and: "It runs the post install script, substituting in the given group and version into the build file"
        def text = new File(appDir, "build.gradle").text
        text.contains("group = \"foo\"")
        text.contains("version = \"0.1\"")
    }

    @Betamax(tape="create-tape")
    def "lazybones passes commandline P args to the root script"() {
        when: "creating a groovyapp with all options passed in"
        def args = [
                "--stacktrace",
                "create",
                "test-tmpl",
                "0.2",
                "groovyappWithArgs",
                "-Pversion=0.2",
                "-Pgroup=bar"]
        def exitCode = runCommand(args, baseWorkDir)

        then: "It successfully completes"
        exitCode == 0

        and: "the gradle build file contains proper group and version"
        def appDir = new File(baseWorkDir, "groovyappWithArgs")
        def text = new File(appDir, "build.gradle").text
        text.contains("group = \"bar\"")
        text.contains("version = \"0.2\"")
    }
}
