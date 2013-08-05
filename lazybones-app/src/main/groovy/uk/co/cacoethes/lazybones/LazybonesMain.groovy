package uk.co.cacoethes.lazybones

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import joptsimple.OptionException
import joptsimple.OptionSet

import java.util.logging.Level
import java.util.logging.LogManager
import joptsimple.OptionParser
import joptsimple.OptionSpec
import uk.co.cacoethes.lazybones.commands.*

import java.util.logging.Logger

@CompileStatic
@Log
class LazybonesMain {

    static final File CONFIG_FILE = new File(System.getProperty('user.home'), '.lazybones/config.groovy')
    static final String DEFAULT_REPOSITORY = 'pledbrook/lazybones-templates'

    static final String USAGE = "USAGE: lazybones [OPTIONS] [COMMAND]\n"

    static ConfigObject configuration

    static void main(String[] args) {
        initConfiguration()

        OptionParser parser = createParser(args)
        OptionSet optionSet
        try {
            optionSet = parser.parse(args)
        }
        catch (OptionException ex) {
            // Logging not initialised yet, so use println()
            println getHelp(ex.message, parser)
            System.exit 1
        }

        // Create a map of options from the "options.*" key in the user's
        // configuration and then add any command line options to that map,
        // overriding existing values.
        def globalOptions = configuration.options ? new HashMap(configuration.options as Map) : [:]
        for (OptionSpec spec in optionSet.specs()) {
            def valueList = spec.values(optionSet)
            globalOptions[spec.options()[0]] = valueList ? valueList[0] : true
        }

        if (optionSet.has("version")) {
            println "Lazybones version ${readVersion()}"
            System.exit 0
        }

        // We're now ready to initialise the logging system as we have the global
        // options parsed and available.
        initLogging(globalOptions)

        // Determine the command to run and its argument list.
        String cmd
        List argsList = optionSet.nonOptionArguments() as List

        if (argsList.size() == 0 || optionSet.has("h")) {
            cmd = "help"
        }
        else {
            cmd = argsList.head()
            argsList = argsList.tail()
        }

        // Execute the corresponding command
        def cmdInstance = Commands.ALL_COMMANDS.find { Command it -> it.name == cmd }
        if (!cmdInstance) {
            log.severe "There is no command '" + cmd + "'"
            System.exit 1
        }

        int retval = cmdInstance.execute(argsList, globalOptions, configuration)
        System.exit retval
    }

    public static String readVersion() {
        def stream = LazybonesMain.getResourceAsStream("lazybones.properties")
        def props = new Properties()
        props.load(stream)
        return props.getProperty("lazybones.version")
    }

    private static void initConfiguration() {
        if (CONFIG_FILE.exists()) {
            configuration = new ConfigSlurper().parse(CONFIG_FILE.toURI().toURL())
        }
        else {
            // Default configuration
            configuration = new ConfigObject()
            configuration.bintrayRepositories = [DEFAULT_REPOSITORY]
        }
    }

    private static void initLogging(Map options) {
        // Load a basic logging configuration from a string containing Java
        // properties syntax.
        def inputStream = new ByteArrayInputStream(LOG_CONFIG.getBytes("UTF-8"))
        LogManager.logManager.readConfiguration(inputStream)

        // Update logging level based on the global options. We temporarily
        // get hold of the parent logger of all Lazybones loggers so that all
        // child loggers are updated (as the child loggers inherit the level
        // from this parent).
        def parentLogger = Logger.getLogger("uk.co.cacoethes.lazybones")

        if (options.v) parentLogger.level = Level.FINEST
        else if (options.quiet) parentLogger.level = Level.WARNING
        else if (options.info) parentLogger.level = Level.INFO
        else if (options.logLevel) {
            try {
                parentLogger.level = Level.parse((String) options.logLevel)
            }
            catch (IllegalArgumentException ex) {
                log.severe "Invalid log level provided: ${ex.message}"
                System.exit 1
            }
        }
    }

    private static OptionParser createParser(String[] args) {
        // These are the global options available for all commands.
        def parser = new OptionParser()
        parser.accepts("stacktrace", "Show stack traces when exceptions are thrown.")
        parser.acceptsAll(["verbose", "v"], "Display extra information when running commands.")
        parser.accepts("quiet", "Show minimal output.")
        parser.accepts("info", "Show normal amount of output (default).")
        parser.accepts("logLevel", "Set logging level, e.g. OFF, SEVERE, INFO, FINE, etc.").withRequiredArg()
        parser.accepts("version", "Print out the Lazybones version and then end.")
        parser.acceptsAll(["help", "h"], "Print out the Lazybones help and then end.")

        // Ensures that only options up to the sub-command ('create, 'list',
        // etc.) are parsed.
        parser.posixlyCorrect(true)
        return parser
    }

    /**
     * Returns a help string to display for usage. It incorporates the given
     * message, the command's usage string, and the supported JOptSimple options.
     */
    protected static String getHelp(String message, OptionParser parser) {
        def writer = new StringWriter()
        parser.printHelpOn(writer)

        return """\
${message}

${USAGE}
${writer}"""
    }

    /**
     * Logging configuration in Properties format. It simply sets up the console
     * handler with a formatter that just prints the message without any decoration.
     */
    private static final String LOG_CONFIG = """\
# Logging
handlers = java.util.logging.ConsoleHandler

# Console logging
java.util.logging.ConsoleHandler.formatter = uk.co.cacoethes.util.PlainFormatter
java.util.logging.ConsoleHandler.level = FINEST
"""
}
