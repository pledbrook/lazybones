package uk.co.cacoethes.lazybones.commands

/**
 *
 */
final class Commands {
    static List<Command> ALL_COMMANDS = Collections.unmodifiableList([
            new CreateCommand(),
            new ListCommand(),
            new InfoCommand(),
            new HelpCommand() ])

    private Commands() {}
}
