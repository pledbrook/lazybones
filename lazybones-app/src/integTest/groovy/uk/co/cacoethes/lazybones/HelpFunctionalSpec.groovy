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

    def "help option prints out the command specific help"() {
        when: "I run lazybones with ${option} for command ${command}"
        def exitCode = runCommand([option, command], baseWorkDir)

        then: "It displays the help"
        exitCode == 0
        output.contains("USAGE")
        output =~ /\s+${command}\s+/
        !(output =~ /Exception/)

        where:
        option  | command
        "-h"    | "list"
        "--help"| "list"
        "-h"    | "info"
        "--help"| "info"
    }

    def "a command with the help option prints the command specific help"() {
        when: "I run lazybones with command ${command} and option ${option}"
        def exitCode = runCommand([command, option], baseWorkDir)

        then: "It displays the help"
        exitCode == 0
        output.contains("USAGE")
        output =~ /\s+${command}\s+/ &&
        !(output =~ /Exception/)
        println output

        where:
        option  | command
        "-h"    | "list"
        "--help"| "list"
        "-h"    | "create"
        "--help"| "create"
    }

    def "help option prints out the help command"() {
        when: "I run lazybones with ${option}"
        def exitCode = runCommand([option], baseWorkDir)

        then: "It displays the help"
        exitCode == 0
        outputContainsHelpMessage()

        where:
        option << ['-h', '--help']

    }

    boolean outputContainsHelpMessage() {
        output.contains("Available commands") &&
        output =~ /\s+create\s+/ &&
        !(output =~ /Exception/)
    }
}
