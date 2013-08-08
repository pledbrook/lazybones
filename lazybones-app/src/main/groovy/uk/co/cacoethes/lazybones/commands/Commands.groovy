package uk.co.cacoethes.lazybones.commands

/**
 *
 */
final class Commands {
    final static List<Command> ALL = Collections.unmodifiableList([
            new CreateCommand(),
            new ListCommand(),
            new InfoCommand(),
            new HelpCommand() ])

    private Commands() {}
}
