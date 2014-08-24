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

    static final String SYSPROP_OVERRIDE_PREFIX = "lazybones."
    static final String ENCODING = "UTF-8"

    static final String USAGE = "USAGE: lazybones [OPTIONS] [COMMAND]\n"

    static void main(String[] args) {
        def config = initConfiguration()

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
        def globalOptions = config.options ? new HashMap(config.options as Map) : [:]
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

    static ConfigObject loadDefaultConfig() {
        def cls = this
        return new ConfigSlurper().parse(cls.getResource("defaultConfig.groovy").text)
    }

    @CompileDynamic
    protected static void validateConfig(ConfigObject configObject) {
        configObject.templates.mappings.each { key, value ->
            if (!UrlUtils.isUrl(value as String)) {
                throw new IllegalArgumentException("the value [$value] for mapping [$key] is not a url")
            }
        }
    }

    /**
     * <ol>
     *   <li>Loads the default configuration file from the classpath</li>
     *   <li>Works out the location of the user config file (either the default
     * or from a system property)</li>
     *   <li>Loads the user config file and merges with the default</li>
     *   <li>Overrides any config options with values provided as system properties</li>
     * </ol>
     * <p>The system properties take the form of 'lazybones.&lt;config.option&gt;'.</p>
     */
    @CompileDynamic
    protected static ConfigObject initConfiguration() {
        def currentConfig = loadDefaultConfig()
        def userConfigFile = (System.getProperty("lazybones.config.file") ?: currentConfig.config.file) as File
        if (userConfigFile.exists()) {
            // User config options override the defaults with this merge()
            currentConfig.merge(new ConfigSlurper().parse(userConfigFile.toURI().toURL()))
        }

        System.properties.findAll { it.key.startsWith(SYSPROP_OVERRIDE_PREFIX) }.each { String key, String value ->
            setConfigOption(currentConfig, key[SYSPROP_OVERRIDE_PREFIX.size()..-1], value)
        }

        // TODO Pretty print the configuration
        log.fine "Current configuration: " + currentConfig
        return currentConfig
    }

    /**
     * <p>Takes a dot-separated string, such as "test.report.dir", and sets the corresponding
     * config object property, {@code root.test.report.dir}, to the given value.</p>
     * <p><em>Note</em> the {@code @CompileDynamic} annotation is currently required due to
     * issue {@link GROOVY-6480 https://jira.codehaus.org/browse/GROOVY-6480}.</p>
     * @param root The config object to set the value on.
     * @param dottedString The dot-separated string representing a configuration option.
     * @param value The new value for this option.
     * @return The ConfigObject containing the final part of the dot-separated string as a
     * key. In other words, {@code retval.dir == value} for the dotted string example above.
     */
    @CompileDynamic
    protected static ConfigObject setConfigOption(ConfigObject root, String dottedString, value) {
        def parts = dottedString.split('\\.')
        def firstParts = parts[0..<(parts.size() - 1)]
        def configEntry = (ConfigObject) firstParts.inject(root) { ConfigObject config, String keyPart ->
            config.getProperty(keyPart)
        }

        configEntry.setProperty(parts[-1], value)
        return configEntry
    }

    private static void initLogging(Map options) {
        // Load a basic logging configuration from a string containing Java
        // properties syntax.
        def inputStream = new ByteArrayInputStream(LOG_CONFIG.getBytes(ENCODING))
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
