package uk.co.cacoethes.lazybones

/**
 * Functional tests for the code explicitly in LazybonesMain that isn't tested
 * implicitly by other functional tests. At the moment, verifies the logging levels.
 */
class MainFunctionalSpec extends AbstractFunctionalSpec {

    def "Error message displayed if command unrecognised"() {
        when: "I run lazybones with an unknown command"
        def exitCode = runCommand([option, "unknown"], baseWorkDir)

        then: "The exit code is non-zero and a simple message is displayed"
        exitCode != 0
        output =~ "There is no command 'unknown'"
        !(output =~ "Exception")

        where:
        option << ["--verbose", "--quiet", "--info", "--logLevel=WARNING"]
    }

    def "Verbose command line option displays extra info"() {
        when: "I run lazybones with the create command and the 'verbose' option"
        def exitCode = runCommand([option, "create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "I see INFO and FINE messages"
        output.contains "Project created in ratapp"
        output.contains "Searching for ratpack in "

        where:
        option << ["-v", "--verbose", "--logLevel=FINER"]
    }

    def "Quiet command line option displays minimal info"() {
        when: "I run lazybones with the create command and the 'quiet' option"
        def exitCode = runCommand([option, "create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "I see only WARNING and SEVERE messages"
        !output.contains("Project created in ratapp")
        !output.contains("Searching for ratpack in ")

        where:
        option << ["-q", "--quiet", "--logLevel=WARNING"]
    }

    def "Info command line option displays normal level of information"() {
        when: "I run lazybones with the create command and the 'info' option"
        def exitCode = runCommand([option, "create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "I see INFO messages"
        output.contains "Project created in ratapp"
        !output.contains("Searching for ratpack in ")

        where:
        option << ["--info", "--logLevel=INFO"]
    }
}
