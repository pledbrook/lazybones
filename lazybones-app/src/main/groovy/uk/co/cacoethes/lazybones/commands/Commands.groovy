package uk.co.cacoethes.lazybones.commands

/**
 *
 */
final class Commands {
    final static List<Command> getAll(ConfigObject config) {
        return Collections.unmodifiableList([
            new CreateCommand(config),
            new ListCommand(),
            new InfoCommand(),
            new HelpCommand(config) ])
    }

    private Commands() { }
}
