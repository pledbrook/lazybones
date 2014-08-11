package uk.co.cacoethes.lazybones.commands

import uk.co.cacoethes.lazybones.config.Configuration

/**
 *
 */
final class Commands {
    final static List<Command> getAll(Configuration config) {
        return Collections.unmodifiableList([
            new CreateCommand(config),
            new ConfigCommand(config),
            new GenerateCommand(),
            new ListCommand(config),
            new InfoCommand(),
            new HelpCommand() ])
    }

    private Commands() { }
}
