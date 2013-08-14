package uk.co.cacoethes.lazybones

import groovy.transform.CompileStatic
import groovy.util.logging.Log

import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.LogManager

import joptsimple.OptionException
import joptsimple.OptionParser
import joptsimple.OptionSet
import joptsimple.OptionSpec

import uk.co.cacoethes.lazybones.Options
import uk.co.cacoethes.lazybones.commands.*


import static uk.co.cacoethes.lazybones.OptionParserBuilder.makeOptionParser

@CompileStatic
@Log
class LazybonesMain {

    static final File CONFIG_FILE = new File(System.getProperty('user.home'), '.lazybones/config.groovy')
    static final String DEFAULT_REPOSITORY = 'pledbrook/lazybones-templates'

    static final String USAGE = "USAGE: lazybones [OPTIONS] [COMMAND]\n"

    static ConfigObject configuration

    static void main(String[] args) {
        initConfiguration()

        OptionParser parser = makeOptionParser()
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

        if (optionSet.has(Options.VERSION)) {
            println "Lazybones version ${readVersion()}"
            System.exit 0
        }

        // We're now ready to initialise the logging system as we have the global
        // options parsed and available.
        initLogging(globalOptions)

        // Determine the command to run and its argument list.
        String cmd
        List argsList = optionSet.nonOptionArguments() as List

        if (argsList.size() == 0 || optionSet.has(Options.HELP_SHORT)) {
            cmd = "help"
        }
        else {
            cmd = argsList.head()
            argsList = argsList.tail()
        }

        // Execute the corresponding command
        def cmdInstance = Commands.ALL.find { Command it -> it.name == cmd }
        if (!cmdInstance) {
            log.severe "There is no command '" + cmd + "'"
            System.exit 1
        }

        int retval = cmdInstance.execute(argsList, globalOptions, configuration)
        System.exit retval
    }

    static String readVersion() {
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
            configuration = new ConfigObject()
        }

        // Set up defaults
        if (!configuration.bintrayRepositories) configuration.bintrayRepositories = [DEFAULT_REPOSITORY]
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

        if (options[Options.VERBOSE_SHORT]) parentLogger.level = Level.FINEST
        else if (options[Options.QUIET]) parentLogger.level = Level.WARNING
        else if (options[Options.INFO]) parentLogger.level = Level.INFO
        else if (options[Options.LOG_LEVEL]) {
            try {
                parentLogger.level = Level.parse((String) options[Options.LOG_LEVEL])
            }
            catch (IllegalArgumentException ex) {
                log.severe "Invalid log level provided: ${ex.message}"
                System.exit 1
            }
        }
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
