package uk.co.cacoethes.lazybones

class HelpFunctionalSpec extends AbstractFunctionalSpec {
    def "A missing command prints the help text"() {
        when: "I run lazybones without any arguments"
        def exitCode = runCommand([], baseWorkDir)

        then: "It displays the help"
        exitCode == 0
        outputContainsHelpMessage()
    }

    def "The help command displays a list of available commands"() {
        when: "I run the help command"
        def exitCode = runCommand(["help"], baseWorkDir)

        then: "It displays the help"
        exitCode == 0
        outputContainsHelpMessage()
    }

    def "-h or --help print out the help command"() {
        when: "I run lazybones with -h or --help"
        def exitCode = runCommand(["-h", "--help"], baseWorkDir)

        then: "It displays the help"
        exitCode == 0
        outputContainsHelpMessage()
    }

    boolean outputContainsHelpMessage() {
        output.contains("Available commands")
        output =~ /\s+create\s+/
        !(output =~ /Exception/)
    }
}
