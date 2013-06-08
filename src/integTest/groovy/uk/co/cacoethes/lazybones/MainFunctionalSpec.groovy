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
        runCommand([option, "create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "I see INFO and FINE messages"
        output.contains "Project created in ratapp"
        output.contains "Searching for ratpack in "

        where:
        option << ["-v", "--verbose", "--logLevel=FINER"]
    }

    def "Quiet command line option displays minimal info"() {
        when: "I run lazybones with the create command and the 'quiet' option"
        runCommand([option, "create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "I see only WARNING and SEVERE messages"
        !output.contains("Project created in ratapp")
        !output.contains("Searching for ratpack in ")

        where:
        option << ["-q", "--quiet", "--logLevel=WARNING"]
    }

    def "Info command line option displays normal level of information"() {
        when: "I run lazybones with the create command and the 'info' option"
        runCommand([option, "create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "I see INFO messages"
        output.contains "Project created in ratapp"
        !output.contains("Searching for ratpack in ")

        where:
        option << ["--info", "--logLevel=INFO"]
    }

    def "The version option prints Lazybones' version"() {
        given: "The version from the lazybones.properties file"
        def stream = getClass().getResourceAsStream("lazybones.properties")
        def props = new Properties()
        props.load(stream)
        def appVersion = props.getProperty("lazybones.version")

        when: "I run lazybones with the version option"
        def exitCode = runCommand(["--version"], baseWorkDir)

        then: "I see the app version printed out"
        exitCode == 0
        output.contains "Lazybones version $appVersion"
    }

    def "An unknown option prints out usage"() {
        when: "I run lazybones with an unknown option"
        def exitCode = runCommand(["--unknown", "help"], baseWorkDir)

        then: "The exit code is non-zero and a usage string is displayed"
        exitCode != 0
        output =~ /'unknown' is not a recognized option/
        output =~ /USAGE: lazybones \[OPTIONS\] \[COMMAND\]/
        !(output =~ /Exception/)

        and: "The message displayed does not include the log level"
        !(output =~ /^SEVERE:\b/)

        where:
        option << ["--verbose", "--quiet", "--info", "--logLevel=WARNING"]
    }
}
