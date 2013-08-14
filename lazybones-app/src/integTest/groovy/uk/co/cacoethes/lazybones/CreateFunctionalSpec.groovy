package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.*

class CreateFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        def proxyAddress = recorder.proxy.address()
        env["JAVA_OPTS"] = "-Dhttps.proxyHost=" + proxyAddress.hostName + " -Dhttps.proxyPort=" + proxyAddress.port +
                " -Dhttp.proxyHost=" + proxyAddress.hostName + " -Dhttp.proxyPort=" + proxyAddress.port
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

        and: "It says that the given version of the package is being installed in the target directory"
        output =~ /Creating project from template ratpack 0.1 in 'ratapp'/
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

        and: "It says that the latest version of the package is being installed in the target directory"
        output =~ /Creating project from template ratpack \(latest\) in 'ratapp'/
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

        and: "It says that the package is being installed in the current directory"
        output =~ /Creating project from template ratpack 0.1 in current directory/
    }

    @Betamax(tape="create-tape")
    def "Create command reports error if no version given and package info is not available"() {
        when: "I run lazybones with the create command for an unknown package and no version"
        def exitCode = runCommand(["create", "unknown", "myapp"], baseWorkDir)

        then: "It returns a non-zero exit code and reports the package as missing"
        exitCode != 0
        output =~ /Cannot find a template named 'unknown'./

        !new File(baseWorkDir, "myapp").exists()
    }

    @Betamax(tape="create-tape")
    def "Create command reports error if package cannot be found"() {
        when: "I run lazybones with the create command for an unknown package"
        def exitCode = runCommand(["create", "unknown", "1.0", "myapp"], baseWorkDir)

        then: "It returns a non-zero exit code and reports the package as missing"
        exitCode != 0
        output =~ /Cannot find a template named 'unknown'./

        !new File(baseWorkDir, "myapp").exists()
    }

    @Betamax(tape="create-tape")
    def "Create command reports error if specified version of a package cannot be found"() {
        when: "I run lazybones with the create command for an unknown version of a known package"
        def exitCode = runCommand(["create", "ratpack", "99.99", "myapp"], baseWorkDir)

        then: "It returns a non-zero exit code and reports the package as missing"
        exitCode != 0
        output =~ /Cannot find version 99.99 of template 'ratpack'./

        !new File(baseWorkDir, "myapp").exists()
    }

    @Betamax(tape="create-tape")
    def "Create command prints useful error message if no versions of a template are available"() {
        when: "I run lazybones with the create command for a template with no versions"
        def exitCode = runCommand(["create", "lazybones-project", "my-lzb-templates"], baseWorkDir)

        then: "It returns a non-zero exit code and displays an error message"
        exitCode == 1
        output =~ /No version of 'lazybones-project' has been published/
    }

    def "Create can install from cache without template being in repository"() {
        when: "I run lazybones with the create command for a template that's only in the cache"
        def exitCode = runCommand(
                ["create", "test-tmpl", "0.2", "testapp", "-Pgroup=foo", "-Pversion=0.1"],
                baseWorkDir)

        then: "It unpacks the template, retaining file permissions"
        exitCode == 0

        def appDir = new File(baseWorkDir, "testapp")
        appDir.exists()
        new File(appDir, "gradlew").canExecute()
        new File(appDir, "src/main/groovy").isDirectory()
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
