package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.*

class ListFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        def proxyAddress = recorder.proxy.address()
        env["JAVA_OPTS"] = "-Dhttps.proxyHost=" + proxyAddress.hostName + " -Dhttps.proxyPort=" + proxyAddress.port
    }

    @Betamax(tape='list-tape')
    def "list command prints all available packages"() {
        when: "I run lazybones with the list command"
        def exitCode = runCommand(["list"], baseWorkDir)

        then: "It displays the available packages as a list"
        exitCode == 0
        output.startsWith("Available mappings")
        output =~ /\s+customRatpack  -> http:\/\/dl.dropboxusercontent.com\/u\/29802534\/custom-ratpack.zip\s+/
        output =~ /\s+doesNotExist   -> file:\/\/\/does\/not\/exist\s+/
        output =~ /\s+groovy-app\s+/
        output =~ /\s+ratpack\s+/
        output =~ /\s+ratpack-lite\s+/
        !(output =~ /Exception/)
    }
}
