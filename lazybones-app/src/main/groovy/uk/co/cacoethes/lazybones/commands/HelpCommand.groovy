package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import joptsimple.OptionSet

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

    private final ConfigObject config

    HelpCommand(ConfigObject config) {
        this.config = config
    }

    @Override
    String getName() { return "help" }

    @Override
    String getDescription() {
        return "Displays general help, or help for a specific command."
    }

    @Override
    protected IntRange getParameterRange() {
        return 0..1
    }

    @Override
    protected String getUsage() { return USAGE }

    @Override
    protected int doExecute(OptionSet cmdOptions, Map globalOptions, ConfigObject config) {
        def cmdArgs = cmdOptions.nonOptionArguments()
        if (cmdArgs) {
            def cmd = Commands.getAll(config).find { Command it -> it.name == cmdArgs[0] }
            if (cmd) {
                println cmd.getHelp(cmd.description)
            }
            else {
                log.severe "There is no command '${cmdArgs[0]}'"
                return 1
            }
        }
        else {
            showGenericHelp()
        }

        return 0
    }

    protected void showGenericHelp() {
        println "Lazybones is a command-line based tool for creating basic software projects from templates."
        println ""
        println "Available commands:"
        println ""
        for (Command cmd in Commands.getAll(config)) {
            println "    " + cmd.name.padRight(15) + cmd.description
        }
        println ""
    }
}
