package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.Rule
import spock.lang.Stepwise

@Stepwise
class LazybonesScriptFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        initProxy(recorder.proxy.address())
    }

    @Betamax(tape="create-tape")
    def "Post-install scripts can access the logger"() {
        when: "I run lazybones with the create command for the groovy-gradle template"
        def exitCode = runCommand(["create", "test-tmpl", "0.2", "groovyapp"], baseWorkDir, ["foo", "", "4"])

        then: "It successfully completes"
        exitCode == 0

        and: "The script's log message is displayed"
        output =~ "User should see this log message"
    }

    @Betamax(tape="create-tape")
    def "lazybones is deleted after package is installed"() {
        given: "The Lazybones version"
        def lazybonesVersion = readLazybonesVersion()

        when: "I run lazybones with the create command for the groovy-gradle template"
        def exitCode = runCommand(["create", "test-tmpl", "0.2", "groovyapp"], baseWorkDir, ["foo", "0.1", "4"])

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
        def exitCode = runCommand(["create", "test-tmpl", "0.2", "groovyapp"], baseWorkDir, ["foo", "", ""])

        then: "It successfully completes, deleting the lazybones script after unpacking the template"
        exitCode == 0
        def appDir = new File(baseWorkDir, "groovyapp")
        !new File(appDir, "lazybones.groovy").exists()

        and: "It runs the post install script, substituting in the given group and version into the build file"
        def text = new File(appDir, "build.gradle").text
        text.contains("group = \"foo\"")
        text.contains("version = \"0.1\"")
        text.contains("test.maxParallelForks = 5")
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
                "-Pgroup=bar",
                "-PmaxThreads=4"]
        def exitCode = runCommand(args, baseWorkDir)

        then: "It successfully completes"
        exitCode == 0

        and: "the gradle build file contains proper group and version"
        def appDir = new File(baseWorkDir, "groovyappWithArgs")
        def text = new File(appDir, "build.gradle").text
        text.contains("group = \"bar\"")
        text.contains("version = \"0.2\"")
    }

    @Betamax(tape="create-tape")
    def "Can use naming utils in post-install script"() {
        given: "A directory to create a new project in"
        def appDir = new File(baseWorkDir, "groovyapp")

        when: "I run lazybones with the create command for the groovy-gradle template"
        def exitCode = runCommand(["create", "test-tmpl", "0.2", "groovyapp"], baseWorkDir, ["foo", "0.1", "4"])

        then: "the post-install script creates a text file containing the appropriate converted names"
        def testText = new File(appDir, "test.txt").text
        testText.contains("TestString")
        testText.contains("A Long Name")
        testText.contains("Missing 'to' argument for transformText()")
    }

    @Betamax(tape="create-tape")
    def "Handlebars templates are processed with Groovy ones"() {
        given: "A directory to create a new project in"
        def appDir = new File(baseWorkDir, "dummy-app")

        and: "A higher command timeout to allow @Grab to finish fetching the JAR"
        commandTimeout = 30000

        when: "I run lazybones with the create command for the handlebars project template"
        def exitCode = runCommand(["create", "test-handlebars", "0.1.1", appDir.name], baseWorkDir)

        then: "the post-install script creates the correct file from a .gtpl source"
        def testText = new File(appDir, "GroovyHello.groovy").text
        testText.contains('println "hello test"')
        testText.contains('println "${bar} was unfiltered"')
        testText.contains('println 100')

        and: "it creates the correct file from a .hbs source"
        def hbsText = new File(appDir, "PrintHello.groovy").text
        hbsText.contains('println "hello test"')
        hbsText.contains('println "${bar} was unfiltered"')
        hbsText.contains('println 100')

        and: "it creates the correct file from a normal source without a template suffix"
        def nmlText = new File(appDir, "NoSuffixGroovyHello.groovy").text
        nmlText.contains('println "hello test"')
        nmlText.contains('println "${bar} was unfiltered"')
        nmlText.contains('println 100')

        and: "the template files are removed"
        !new File(appDir, "GroovyHello.groovy.gtpl").exists()
        !new File(appDir, "PrintHello.groovy.hbs").exists()
    }

    @Betamax(tape="create-tape")
    def "Non-suffixed templates are processed with Handlebars engine"() {
        given: "A directory to create a new project in"
        def appDir = new File(baseWorkDir, "dummy-app2")

        and: "A reset command timeout"
        commandTimeout = 10000

        when: "I run lazybones with the create command for the handlebars project template"
        def exitCode = runCommand(["create", "test-handlebars-default", "0.1", appDir.name], baseWorkDir)

        then: "the post-install script creates the correct file from a .gtpl source"
        def testText = new File(appDir, "GroovyHello.groovy").text
        testText.contains('println "hello test"')
        testText.contains('println "${bar} was unfiltered"')
        testText.contains('println 100')

        and: "it creates the correct file from a .hbs source"
        def hbsText = new File(appDir, "PrintHello.groovy").text
        hbsText.contains('println "hello test"')
        hbsText.contains('println "${bar} was unfiltered"')
        hbsText.contains('println 100')

        and: "it creates the correct file from a normal source without a template suffix"
        def nmlText = new File(appDir, "NoSuffixGroovyHello.groovy").text
        nmlText.contains('println "hello test"')
        nmlText.contains('println "${bar} was unfiltered"')
        nmlText.contains('println 100')
    }

    @Betamax(tape="create-tape")
    def "Disabling default template engine removes processing of non-suffixed files"() {
        given: "A directory to create a new project in"
        def appDir = new File(baseWorkDir, "dummy-app3")

        when: "I run lazybones with the create command for the handlebars project template"
        def exitCode = runCommand(["create", "test-no-default-engine", "0.1", appDir.name], baseWorkDir)

        then: "the post-install script creates the correct file from a .gtpl source"
        def testText = new File(appDir, "GroovyHello.groovy").text
        testText.contains('println "hello test"')
        testText.contains('println "${bar} was unfiltered"')
        testText.contains('println 100')

        and: "it creates the correct file from a .hbs source"
        def hbsText = new File(appDir, "PrintHello.groovy").text
        hbsText.contains('println "hello test"')
        hbsText.contains('println "${bar} was unfiltered"')
        hbsText.contains('println 100')

        and: "it copies a normal source without a template suffix, rather than processing it"
        def nmlText = new File(appDir, "NoSuffixGroovyHello.groovy").text
        nmlText.contains('println "hello ${foo}"')
        nmlText.contains('println "\\${bar} was unfiltered"')
        nmlText.contains('println ${bar}')

        and: "the template files are removed"
        !new File(appDir, "GroovyHello.groovy.gtpl").exists()
        !new File(appDir, "PrintHello.groovy.hbs").exists()
    }
}
