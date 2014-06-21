package uk.co.cacoethes.lazybones.commands

/**
 *
 */
final class Commands {
    final static List<Command> getAll(ConfigObject config) {
        return Collections.unmodifiableList([
            new CreateCommand(config),
            new GenerateCommand(),
            new ListCommand(config),
            new InfoCommand(),
            new HelpCommand(config) ])
    }

    private Commands() { }
}
