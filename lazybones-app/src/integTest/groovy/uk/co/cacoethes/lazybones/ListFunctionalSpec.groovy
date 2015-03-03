package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.apache.commons.io.FileUtils
import org.junit.*

class ListFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        def proxyAddress = recorder.proxy.address()
        env["JAVA_OPTS"] = "-Dhttps.proxyHost=" + proxyAddress.hostName + " -Dhttps.proxyPort=" + proxyAddress.port
    }

    @Betamax(tape='list-tape')
    def "list command prints all available packages"() {
        given: "An expected list of packages"
        def expected = ["afterburnerfx", "dropwizard", "gaelyk", "gradle-plugin", "gradle-quickstart"]

        when: "I run lazybones with the list command"
        def exitCode = runCommand(["list"], baseWorkDir)

        then: "It displays the available packages as a list"
        exitCode == 0
        output =~ /(?m)^Available mappings\s+/ +
                /\s+customRatpack  -> http:\/\/dl.dropboxusercontent.com\/u\/29802534\/custom-ratpack.zip\s+/ +
                /\s+doesNotExist   -> file:\/\/\/does\/not\/exist\s+/
        output =~ /\s+${expected.join('\\s+')}\s+/
        !(output =~ /Exception/)
        !(output =~ /Cached templates/)
    }

    @Betamax(tape='list-tape')
    def "list command prints cached templates"() {
        given: "A list of remote packages"
        def remoteTmpls = ["afterburnerfx", "dropwizard", "gaelyk", "gradle-plugin", "gradle-quickstart"]

        and: "A template in the cache matching a remote one"
        def ratpackPackage = new File(cacheDirPath, "ratpack-0.1.zip")
        if (!ratpackPackage.exists()) {
            FileUtils.touch(ratpackPackage)
            filesToDelete << ratpackPackage
        }

        when: "I run the list command with --cached option"
        def exitCode = runCommand(["list", "--cached"], baseWorkDir)

        then: "It displays the available packages as a list"
        exitCode == 0
        output =~ /(?m)^Available mappings\s+/ +
                /\s+customRatpack  -> http:\/\/dl.dropboxusercontent.com\/u\/29802534\/custom-ratpack.zip\s+/ +
                /\s+doesNotExist   -> file:\/\/\/does\/not\/exist\s+/

        output =~ /(?m)^Cached templates\s+/ +
                /Oops-stuff                    1.0.4\s+/ +
                /ratpack                       0.1\s+/ +
                /subtemplates-tmpl             0.1\s+/ +
                /test-handlebars               0.1.1/
        !(output =~ /Exception/)
        !(remoteTmpls.any { it in output })
    }

    @Betamax(tape='list-tape')
    def "list command prints no sub-templates"() {
        given: "A lazybones generated project"
        def projectDir = new File(baseWorkDir, 'project')
        def localLazybonesDir = new File(projectDir, '.lazybones')
        localLazybonesDir.mkdirs()

        filesToDelete << localLazybonesDir
        filesToDelete << projectDir

        when: "I run the list command with --cached option"
        // do not clear workdir or we loose our setup
        def exitCode = runCommand(["list", "--cached"], projectDir, [], false)

        then: "It does not display any available sub-templates"
        exitCode == 0
        !(output =~ /(?m)^Available sub-templates\s+/)
        !(output =~ /Exception/)
    }

    @Betamax(tape='list-tape')
    def "list command prints available sub-templates"() {
        given: "A lazybones generated project"
        def projectDir = new File(baseWorkDir, 'project')
        def localLazybonesDir = new File(projectDir, '.lazybones')
        localLazybonesDir.mkdirs()

        and: "A sub-template is available in the local lazybones directory"
        def templateFile = new File(localLazybonesDir, 'artifact-template-1.0.0.zip')
        templateFile.text = '''
        Any content will do. We check for the file's existence
        and its filename pattern only.
        '''

        filesToDelete << templateFile
        filesToDelete << localLazybonesDir
        filesToDelete << projectDir

        when: "I run the list command with --cached option"
        // do not clear workdir or we loose our setup
        def exitCode = runCommand(["list", "--cached"], projectDir, [], false)

        then: "It does display all available sub-templates"
        exitCode == 0
        output =~ /(?m)^Available sub-templates\s+/ +
                  /\s+artifact\s+/

        !(output =~ /Exception/)
    }
}
