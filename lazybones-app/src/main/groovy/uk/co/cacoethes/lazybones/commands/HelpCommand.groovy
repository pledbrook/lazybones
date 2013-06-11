package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log

/**
 *
 */
@CompileStatic
@Log
class HelpCommand extends AbstractCommand {
    static final String USAGE = """\
USAGE: help <cmd>?

  where  cmd = The name of the command to show help for. If not specified,
               the command displays the generic Lazybones help.
"""

    @Override
    String getName() { return "help" }

    @Override
    String getDescription() {
        return "Displays general help, or help for a specific command."
    }

    @Override
    int execute(List<String> args, Map globalOptions, ConfigObject config) {
        def cmdOptions = parseArguments(args, 0..1)
        if (!cmdOptions) return 1

        def cmdArgs = cmdOptions.nonOptionArguments()
        if (!cmdArgs) {
            showGenericHelp()
        }
        else {
            def cmd = Commands.ALL_COMMANDS.find { Command it -> it.name == cmdArgs[0] }
            if (!cmd) {
                log.severe "There is no command '${cmdArgs[0]}'"
                return 1
            }
            else {
                println cmd.getHelp(cmd.description)
            }
        }

        return 0
    }

    protected void showGenericHelp() {
        println "Lazybones is a command-line based tool for creating basic software projects from templates."
        println ""
        println "Available commands:"
        println ""
        for (Command cmd in Commands.ALL_COMMANDS) {
            println "    " + cmd.name.padRight(15) + cmd.description
        }
        println ""
    }

    @Override
    protected String getUsage() { return USAGE }
}
