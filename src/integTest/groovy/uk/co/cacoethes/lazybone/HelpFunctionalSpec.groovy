package uk.co.cacoethes.lazybones

class HelpFunctionalSpec extends AbstractFunctionalSpec {
    def "A missing command prints the help text"() {
        when: "I run lazybones without any arguments"
        def exitCode = runCommand([], baseWorkDir)

        then: "It displays the help"
        exitCode == 0
        output.contains("Available commands")
        output =~ /\s+create\s+/
        !(output =~ /Exception/)
    }

    def "The help command displays a list of available commands"() {
        when: "I run the help command"
        def exitCode = runCommand(["help"], baseWorkDir)

        then: "The command successfully completes and I see a list of the available commands"
        exitCode == 0
        output.contains("Available commands")
        output =~ /\s+create\s+/
        !(output =~ /Exception/)
    }
}
