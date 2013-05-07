package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.*

class CreateFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        def proxyAddress = recorder.proxy.address()
        env["JAVA_OPTS"] = "-Dhttps.proxyHost=" + proxyAddress.hostName + " -Dhttps.proxyPort=" + proxyAddress.port
    }

    @Betamax(tape="create-tape")
    def "Create command installs a packaged template"() {
        when: "I run lazybones with the create command for the ratpack template"
        def exitCode = runCommand(["create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "It unpacks the template, retaining file permissions"
        exitCode == 0

        def appDir = new File(baseWorkDir, "ratapp")
        appDir.exists()
        new File(appDir, "gradlew").canExecute()
        new File(appDir, "src/main/groovy").isDirectory()
        new File(appDir, "src/ratpack/public/index.html").isFile()
    }

    @Betamax(tape="create-tape")
    def "Create command installs latest version of a packaged template if version not specified"() {
        when: "I run lazybones with the create command for the ratpack template"
        def exitCode = runCommand(["create", "ratpack", "ratapp"], baseWorkDir)

        then: "It unpacks the template, retaining file permissions"
        exitCode == 0

        def appDir = new File(baseWorkDir, "ratapp")
        appDir.exists()
        new File(appDir, "gradlew").canExecute()
        new File(appDir, "src/main/groovy").isDirectory()
        new File(appDir, "src/ratpack/public/index.html").isFile()
    }

    @Betamax(tape="create-tape")
    def "Create command installs a packaged template into current directory"() {
        given: "An existing application directory"
        def appDir =  new File(baseWorkDir, "ratapp2")
        appDir.mkdirs()

        when: "I run lazybones with the create command for the ratpack template in the app directory with '.'"
        def exitCode = runCommand(["create", "ratpack", "0.1", "."], appDir)

        then: "It unpacks the template, retaining file permissions"
        exitCode == 0

        appDir.exists()
        new File(appDir, "gradlew").canExecute()
        new File(appDir, "src/main/groovy").isDirectory()
        new File(appDir, "src/ratpack/public/index.html").isFile()
    }

    def "Create command displays usage when incorrect number of arguments are provided"() {
        when: "I run lazybones with the create command without an extra argument"
        def exitCode = runCommand(["create"], baseWorkDir)

        then: "It returns a non-zero exit code and displays an error message"
        exitCode == 1
        output =~ /Incorrect number of arguments/
        output =~ /USAGE:/
    }
}
