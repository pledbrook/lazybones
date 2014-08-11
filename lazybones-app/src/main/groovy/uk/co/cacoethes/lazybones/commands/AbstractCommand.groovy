package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import joptsimple.OptionException
import joptsimple.OptionParser
import joptsimple.OptionSet
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.lazybones.Options

/**
 * Base class for most command implementations. It mostly provides help with
 * parsing extra command-specific options.
 */
@CompileStatic
@Log
@SuppressWarnings('FactoryMethodName')
abstract class AbstractCommand implements Command {
    @Override
    @SuppressWarnings('UnnecessaryGetter')
    int execute(List<String> args, Map globalOptions, Configuration config) {
        OptionSet cmdOptions = parseArguments(args, parameterRange)
        if (!cmdOptions) return 1

        if (cmdOptions.has(Options.HELP_SHORT)) {
            println getHelp(getDescription())
            return 0
        }

        return doExecute(cmdOptions, globalOptions, config)
    }

    protected abstract int doExecute(OptionSet optionSet, Map globalOptions, Configuration config)

    /**
     * Returns the number of arguments this command can accept, on top of the
     * default ones handled by this class, such as {@code -h/--help}.
     */
    protected abstract IntRange getParameterRange()

    /**
     * Returns the USAGE string for this command.
     */
    protected abstract String getUsage()

    /**
     * Creates a JOptSimple parser. This should return a parser that is already
     * configured with the options supported by the command. By default this
     * returns an empty parser without any defined options.
     */
    protected OptionParser createParser() {
        OptionParser parser = new OptionParser()
        parser.acceptsAll([Options.HELP_SHORT, Options.HELP],  "Displays usage.")

        return doAddToParser(parser)
    }

    protected OptionParser doAddToParser(OptionParser parser) { return parser }

    /**
     * Uses the parser from {@link AbstractCommand#createParser()} to parse the
     * part of the command line specific to this command and returns a JOptSimple
     * set of parsed options.
     * @param args The command line arguments given to this command.
     * @param validArgCount A range specifying how many non-option arguments
     * (those that don't begin with '-' or '--') can be provided to the command.
     * If the number of non-option arguments falls outside this range, the method
     * returns null and prints an error to the console.
     * @return The option set, or {@code null} if the arguments can't be parsed
     * for whatever reason
     *
     * TODO This should probably throw exceptions in the case of errors. Too much
     * information is lost with a {@code null} return.
     */
    @SuppressWarnings('ReturnNullFromCatchBlock')
    protected OptionSet parseArguments(List<String> args, IntRange validArgCount) {
        try {
            def options = createParser().parse(args as String[])

            if (!(options.nonOptionArguments().size() in validArgCount) && !options.has(Options.HELP_SHORT)) {
                log.severe getHelp("Incorrect number of arguments.")
                return null
            }

            return options
        }
        catch (OptionException ex) {
            log.severe getHelp(ex.message)
            return null
        }
    }

    /**
     * Returns a help string to display for usage. It incorporates the given
     * message, the command's usage string, and the supported JOptSimple options.
     */
    String getHelp(String message) {
        def writer = new StringWriter()
        createParser().printHelpOn(writer)

        return """\
${message}

${usage}
${writer}"""
    }
}
