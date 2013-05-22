package uk.co.cacoethes.lazybones

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import java.util.logging.Level
import java.util.logging.LogManager
import joptsimple.OptionParser
import joptsimple.OptionSet
import joptsimple.OptionSpec
import org.codehaus.groovy.control.CompilerConfiguration
import uk.co.cacoethes.lazybones.commands.*
import uk.co.cacoethes.util.ArchiveMethods

import java.util.logging.Logger

@CompileStatic
@Log
class LazybonesMain {

    static final File CONFIG_FILE = new File(System.getProperty('user.home'), '.lazybones/config.groovy')
    static final String DEFAULT_REPOSITORY = 'pledbrook/lazybones-templates'

    static ConfigObject configuration

    static void main(String[] args) {
        initConfiguration()
        def optionSet = parseArguments(args)

        // Create a map of options from the "options.*" key in the user's
        // configuration and then add any command line options to that map,
        // overriding existing values.
        def globalOptions = configuration.options ? new HashMap(configuration.options as Map) : [:]
        for (OptionSpec spec in optionSet.specs()) {
            def valueList = spec.values(optionSet)
            globalOptions[spec.options()[0]] = valueList ? valueList[0] : true
        }

        // We're now ready to initialise the logging system as we have the global
        // options parsed and available.
        initLogging(globalOptions)

        // Determine the command to run and its argument list.
        String cmd
        List argsList = optionSet.nonOptionArguments() as List
        if (argsList.size() == 0) {
            cmd = "help"
            argsList = []
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

        int retval = (int) cmdInstance.execute(argsList, globalOptions, configuration)
        System.exit retval
    }

    private static void initConfiguration() {
        if (CONFIG_FILE.exists()) {
            configuration = new ConfigSlurper().parse(CONFIG_FILE.toURL())
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

        if (options.verbose) parentLogger.level = Level.FINEST
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

    private static OptionSet parseArguments(String[] args) {
        // These are the global options available for all commands.
        def parser = new OptionParser()
        parser.accepts("stacktrace", "Show stack traces when exceptions are thrown.")
        parser.accepts("verbose", "Display extra information when running commands.")
        parser.accepts("quiet", "Show minimal output.")
        parser.accepts("info", "Show normal amount of output (default).")
        parser.accepts("logLevel", "Set logging level, e.g. OFF, SEVERE, INFO, FINE, etc.").withRequiredArg()

        // Ensures that only options up to the sub-command ('create, 'list',
        // etc.) are parsed.
        parser.posixlyCorrect(true)
        return parser.parse(args)
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
