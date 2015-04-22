package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.*

class InfoFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        initProxy(recorder.proxy.address())
    }

    @Betamax(tape='info-tape')
    def "Info command prints all available info for a package"() {
        when: "I run lazybones with the info command for the ratpack template"
        def exitCode = runCommand(["info", "ratpack"], baseWorkDir)

        then: "It displays the ratpack info"
        exitCode == 0
        output =~ /\bName:\s+ratpack\b/
        output =~ /\bLatest:\s+0.1\b/
        output =~ /\bOwner:\s+pledbrook\b/
        output =~ /\bVersions:\s+0.1\b/
        output =~ /\bDescription:\s+Template for a basic Groovy-based Ratpack application/
        !(output =~ /Exception/)
    }

    @Betamax(tape='info-tape')
    def "Info command prints only info that is available for a package, when that is just a subset of all info"() {
        when: "I run lazybones with the info command for the ratpack-lite template"
        def exitCode = runCommand(["info", "ratpack-lite"], baseWorkDir)

        then: "It displays the ratpack info"
        exitCode == 0
        output =~ /\bName:\s+ratpack-lite\b/
        output =~ /\bLatest:\s+0.1\b/
        output =~ /\bOwner:\s+pledbrook\b/
        output =~ /\bVersions:\s+0.1\b/
        !(output =~ /\bDescription:\s+/)
        !(output =~ /Exception/)
    }

    @Betamax(tape='info-tape')
    def "Info command notifies user when a package can't be found"() {
        when: "I run lazybones with the info command for a non-existent template"
        def exitCode = runCommand(["info", "dummy"], baseWorkDir)

        then: "It returns a non-zero exit code and displays an error message"
        exitCode == 1
        output =~ /Cannot find a template named 'dummy'/
    }

    @Betamax(tape='info-tape')
    def "Info command prints useful error message if no versions of a template are available"() {
        when: "I run lazybones with the info command for a template with no versions"
        def exitCode = runCommand(["info", "lazybones-project"], baseWorkDir)

        then: "It returns a non-zero exit code and displays an error message"
        exitCode == 1
        output =~ /No version of 'lazybones-project' has been published/
    }

    def "Info command displays usage when incorrect number of arguments are provided"() {
        when: "I run lazybones with the info command without an extra argument"
        def exitCode = runCommand(["info"], baseWorkDir)

        then: "It returns a non-zero exit code and displays an error message"
        exitCode == 1
        output =~ /Incorrect number of arguments/
        output =~ /USAGE:/
    }
}
