package uk.co.cacoethes.lazybones

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Log

import java.util.jar.Manifest
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.LogManager

import joptsimple.OptionException
import joptsimple.OptionParser
import joptsimple.OptionSet
import joptsimple.OptionSpec

import uk.co.cacoethes.lazybones.commands.Command
import uk.co.cacoethes.lazybones.commands.Commands
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.util.UrlUtils

import static uk.co.cacoethes.lazybones.OptionParserBuilder.makeOptionParser

/**
 * This is the main entry point for the command line Lazybones application. It
 * handles the command line arguments and offloads the work to the relevant
 * {@link Command} implementation.
 */
@CompileStatic
@Log
class LazybonesMain {

    static final String USAGE = "USAGE: lazybones [OPTIONS] [COMMAND]\n"

    static void main(String[] args) {
        def config = Configuration.initConfiguration()
//        def settings = config.settings

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
        def globalOptions = config.getSubSettings("options") ?: [:]
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

        validateConfig(config)

        // Execute the corresponding command
        def cmdInstance = Commands.getAll(config).find { Command it -> it.name == cmd }
        if (!cmdInstance) {
            log.severe "There is no command '" + cmd + "'"
            System.exit 1
        }

        int retval = cmdInstance.execute(argsList, globalOptions, config)
        System.exit retval
    }

    static String readVersion() {
        // First find the MANIFEST.MF for the JAR containing this class
        //
        // Can't use this.getResource() since that looks for a static method
        def cls = this
        def classPath = cls.getResource(cls.simpleName + ".class").toString()
        if (!classPath.startsWith("jar")) return "unknown"

        def manifestPath = classPath[0..classPath.lastIndexOf("!")] + "/META-INF/MANIFEST.MF"

        // Now read the manifest and extract Implementation-Version to get the
        // Lazybones version.
        def manifest = new Manifest(new URL(manifestPath).openStream())
        return manifest.mainAttributes.getValue("Implementation-Version")
    }

    @CompileDynamic
    protected static void validateConfig(Configuration config) {
        config.getSubSettings("templates.mappings").each { key, value ->
            if (!UrlUtils.isUrl(value as String)) {
                throw new IllegalArgumentException("the value [$value] for mapping [$key] is not a url")
            }
        }
    }

    private static void initLogging(Map options) {
        // Load a basic logging configuration from a string containing Java
        // properties syntax.
        def inputStream = new ByteArrayInputStream(LOG_CONFIG.getBytes(Configuration.ENCODING))
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
