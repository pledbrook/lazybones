package uk.co.cacoethes.lazybones.config

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Log

import java.net.Authenticator.RequestorType
import java.util.logging.Level

/**
 * <p>Central configuration for Lazybones, although only the static data and methods
 * are Lazybones specific. The instance-level API works on the basis of settings
 * whose names are dot-separated and supplied from various sources: a base
 * configuration (typically hard-coded in the app), an app-managed JSON file that
 * can be updated at runtime, and a user-defined configuration.</p>
 * <p>The user-defined configuration takes precedence over the app-managed one,
 * which in turn takes precedence over the base config. When settings are changed
 * at runtime, the class attempts to warn the caller if any setting change will be
 * overridden by an existing user-defined value.</p>
 * <p>This class also maintains a list of valid settings: if it comes across any
 * settings that aren't in the list, it will throw an exception. It will also throw
 * an exception if the value of a setting isn't of the registered type. This ensures
 * that a user can get quick feedback about typos and other errors in their config.
 * </p>
 */
@Log
@SuppressWarnings("MethodCount")
class Configuration {

    static final String SYSPROP_OVERRIDE_PREFIX = "lazybones."
    static final String ENCODING = "UTF-8"
    static final String JSON_CONFIG_FILENAME = "managed-config.json"

    static final Map<String, Class> VALID_OPTIONS
    static final String CONFIG_FILE_SYSPROP = "lazybones.config.file"
    static final String NAME_SEPARATOR = "."
    static final String NAME_SEPARATOR_REGEX = "\\."

    static {
        def options = [
                "config.file": String,
                "cache.dir": String,
                "git.name": String,
                "git.email": String,
                "options.logLevel": String,
                "options.verbose": Boolean,
                "options.quiet": Boolean,
                "options.info": Boolean,
                "bintrayRepositories": String[],
                "templates.mappings.*": URI,
                "systemProp.*": Object]

        // These settings should only be active for the functional tests, not when the
        // application is being used normally.
        if (System.getProperty(CONFIG_FILE_SYSPROP)?.endsWith("test-config.groovy")) {
            options << [
                    "test.my.option": Integer,
                    "test.option.override": String,
                    "test.option.array": String[],
                    "test.integer.array": Integer[],
                    "test.other.array": String[],
                    "test.adding.array": String[]
            ]
        }

        VALID_OPTIONS = Collections.unmodifiableMap(options)
    }

    private final File jsonConfigFile
    private final ConfigObject settings
    private final ConfigObject managedSettings
    private final Map overrideSettings
    private final Map validOptions

    protected Configuration(
            ConfigObject baseSettings,
            Map overrideSettings,
            Map managedSettings,
            Map validOptions,
            File jsonConfigFile) {
        this.validOptions = validOptions
        this.overrideSettings = overrideSettings
        this.jsonConfigFile = jsonConfigFile

        this.settings = baseSettings
        addConfigEntries managedSettings, this.settings
        addConfigEntries overrideSettings, this.settings

        this.managedSettings = new ConfigObject()
        addConfigEntries managedSettings, this.managedSettings

        processSystemProperties(this.settings)

        // Validate the provided settings to ensure that they are known and that
        // they have a value of the appropriate type.
        def invalidOptions = this.settings.flatten().findAll { key, value ->
            !validateSetting(key, validOptions, value)
        }.keySet()

        if (invalidOptions) {
            throw new MultipleInvalidSettingsException(invalidOptions as List)
        }

        // If there are no problems, it's time to enable support for HTTP proxies
        // that require authentication.
        Authenticator.setDefault(new ProxyAuthenticator())
    }

    /** Returns the location of the managed config file (stored as JSON). */
    File getJsonConfigFile() { return jsonConfigFile }

    /**
     * Persists the managed config settings as JSON to the file located by
     * {@link #jsonConfigFile}.
     * @return A list of the keys in the managed config settings that are
     * overridden by values in the user config and system properties
     * (represented by a map of override settings).
     */
    List storeSettings() {
        def sharedKeys = findIntersectKeys(managedSettings, overrideSettings)
        jsonConfigFile.setText(new JsonBuilder(managedSettings).toPrettyString(), ENCODING)

        return sharedKeys
    }

    /**
     * Retrieves the value of a setting by name.
     * @param name The name of the setting as a dot-separated string.
     * @return The current value of the requested setting.
     * @throws UnknownSettingException If the setting name is not recognised,
     * i.e. it isn't in the registered list of known settings.
     */
    def getSetting(String name) {
        requireSettingType(name)
        return getConfigOption(this.settings, name)
    }

    /**
     * Retrieves a parent key from the current settings. This method won't work
     * for complete setting names (as defined in the known settings/valid
     * options map). For example, if the configuration has multiple 'repo.url.*'
     * entries, you can get all of them in one go by passing 'repo.url' to this
     * method.
     * @param rootSettingName The partial setting name (dot-separated) that you
     * want.
     * @return A map of the keys under the given setting name. The map will be empty
     * if there are no settings under the given key.
     * @throws UnknownSettingException If the partial setting name doesn't match
     * any of the settings in the known settings map.
     * @throws InvalidSettingException If the setting name is not partial but
     * is a complete match for an entry in the known settings map.
     */
    Map getSubSettings(String rootSettingName) {
        if (validOptions.containsKey(rootSettingName)) {
            throw new InvalidSettingException(rootSettingName, null, "'$rootSettingName' has no sub-settings")
        }

        final foundMatching = validOptions.any { pattern, valueType ->
            pattern.startsWith(rootSettingName + NAME_SEPARATOR) ||
                rootSettingName ==~ settingNameAsRegex(pattern)
        }
        if (!foundMatching) throw new UnknownSettingException(rootSettingName)

        return getConfigOption(this.settings, rootSettingName) as Map ?: [:]
    }

    /**
     * Returns all the current settings as a flat map. In other words, the keys
     * are the dot-separated names of the settings and there are no nested maps.
     * It's similar to converting the hierarchical ConfigObject into a Properties
     * object.
     */
    Map getAllSettings() {
        return settings.flatten()
    }

    /**
     * Adds a new setting to the current configuration or updates the value of
     * an existing setting.
     * @param name The name of the setting to add/update (in dot-separated form).
     * @param value The new value for the setting. This value may be a
     * {@code CharSequence}, in which case this method attempts to convert it to
     * the appropriate type for the specified setting.
     * @return {@code true} if the new value is <b>not</b> overridden by the
     * existing user-defined configuration. If {@code false}, the new value will
     * take effect immediately, but won't survive the recreation of the {@link
     * Configuration} class.
     * @throws UnknownSettingException If the setting name doesn't match any of
     * those in the known settings map.
     * @throws InvalidSettingException If the given value is not of the appropriate
     * type for this setting, or if it cannot be converted to the correct type from
     * a string.
     */
    boolean putSetting(String name, value) {
        def settingType = requireSettingType(name)

        def convertedValue
        try {
            convertedValue = value instanceof CharSequence ?
                    Converters.getConverter(settingType).toType(value) :
                    requireValueOfType(name, value, settingType)
        }
        catch (all) {
            log.log Level.FINEST, all.message, all
            throw new InvalidSettingException(name, value)
        }

        setConfigOption(settings, name, convertedValue)
        setConfigOption(managedSettings, name, convertedValue)
        return getConfigOption(overrideSettings, name) == null
    }

    /**
     * Adds an extra value to an array/list setting.
     * @param name The dot-separated setting name to modify.
     * @param value The new value to add. If this is a {@code CharSequence}
     * then it is converted to the appropriate type.
     * @return {@code true} if the new value is <b>not</b> overridden by the
     * existing user-defined configuration. If {@code false}, the new value will
     * take effect immediately, but won't survive the recreation of the {@link
     * Configuration} class.
     * @throws UnknownSettingException If the setting name doesn't match any of
     * those in the known settings map.
     * @throws InvalidSettingException If the given value is not of the appropriate
     * type for this setting, or if it cannot be converted to the correct type from
     * a string.
     */
    boolean appendToSetting(String name, value) {
        def settingType = requireSettingType(name)
        if (!settingType.isArray()) {
            throw new InvalidSettingException(
                    name, value,
                    "Setting '${name}' is not an array type, so you cannot add to it")
        }

        def convertedValue = value instanceof CharSequence ?
                Converters.getConverter(settingType.componentType).toType(value) :
                requireValueOfType(name, value, settingType.componentType)

        getConfigOptionAsList(settings, name) << convertedValue
        getConfigOptionAsList(managedSettings, name) << convertedValue
        return getConfigOption(overrideSettings, name) == null
    }

    /**
     * Removes all values from an array/list setting.
     * @param name The dot-separated name of the setting to clear.
     * @throws UnknownSettingException If the setting name doesn't match any of
     * those in the known settings map.
     */
    void clearSetting(String name) {
        requireSettingType(name)

        clearConfigOption(settings, name)
        clearConfigOption(managedSettings, name)
    }

    /**
     * Takes any config settings under the key "systemProp" and converts them
     * into system properties. For example, a "systemProp.http.proxyHost" setting
     * becomes an "http.proxyHost" system property in the current JVM.
     * @param config The configuration to load system properties from.
     */
    protected void processSystemProperties(ConfigObject config) {
        config.systemProp.flatten().each { name, value ->
            System.setProperty(name, value?.toString())
        }
    }

    protected Class requireSettingType(String name) {
        def settingType = getSettingType(name, validOptions)
        if (!settingType) {
            throw new UnknownSettingException(name)
        }
        return settingType
    }

    protected requireValueOfType(String name, value, Class settingType) {
        if (valueOfType(value, settingType)) {
            return value
        }
        else {
            throw new InvalidSettingException(name, value)
        }
    }

    protected boolean valueOfType(value, Class settingType) {
        if (settingType.isArray() && value instanceof List) {
            return value.every { settingType.componentType.isAssignableFrom(it.getClass()) }
        }
        else {
            return settingType.isAssignableFrom(value.getClass())
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
    static Configuration initConfiguration() {
        def defaultConfig = loadDefaultConfig()
        def userConfigFile = (System.getProperty(CONFIG_FILE_SYSPROP) ?: defaultConfig.config.file) as File
        def jsonConfigFile = getJsonConfigFile(userConfigFile)

        return initConfiguration(
                defaultConfig,
                userConfigFile.exists() ? userConfigFile.newReader(ENCODING) : new StringReader(""),
                jsonConfigFile)
    }

    static Configuration initConfiguration(ConfigObject baseConfig, Reader userConfigSource, File jsonConfigFile) {
        def jsonConfig = [:]
        if (jsonConfigFile?.exists()) {
            jsonConfig = loadJsonConfig(jsonConfigFile.newReader(ENCODING))
        }

        def overrideConfig = loadConfig(userConfigSource)

        // Load settings from system properties. These override all other sources.
        loadConfigFromSystemProperties(overrideConfig)

        return new Configuration(baseConfig, overrideConfig, jsonConfig, VALID_OPTIONS, jsonConfigFile)
    }

    static Class getSettingType(String name, Map knownSettings) {
        def valueType = knownSettings[name] ?: knownSettings[makeWildcard(name)]
        return valueType
    }

    @SuppressWarnings("DuplicateNumberLiteral")
    static String makeWildcard(String dottedString) {
        if (dottedString.indexOf(NAME_SEPARATOR) == -1) return dottedString
        else return dottedString.split(NAME_SEPARATOR_REGEX)[0..-2].join(NAME_SEPARATOR) + ".*"
    }

    static Map.Entry matchingSetting(String name, Map knownSettings) {
        return knownSettings.find { String key, value ->
            key == name || name =~ settingNameAsRegex(key)
        }
    }

    protected static String settingNameAsRegex(String name) {
        return name.replace(NAME_SEPARATOR, NAME_SEPARATOR_REGEX).replace("*", "[\\w]+")
    }

    /**
     * Checks whether
     * @param name
     * @param knownSettings
     * @param value
     * @return
     */
    static boolean validateSetting(String name, Map knownSettings, value) {
        def setting = matchingSetting(name, knownSettings)
        if (!setting) throw new UnknownSettingException(name)

        def converter = Converters.getConverter(setting.value)
        return value == null || converter.validate(value)
    }

    /**
     * Parses Groovy ConfigSlurper content and returns the corresponding
     * configuration as a ConfigObject.
     */
    static ConfigObject loadConfig(Reader r) {
        return new ConfigSlurper().parse(r.text)
    }

    static Map loadJsonConfig(Reader r) {
        return new JsonSlurper().parse(r) as Map
    }

    /**
     * <p>Takes a dot-separated string, such as "test.report.dir", and gets the corresponding
     * config object property, {@code root.test.report.dir}.</p>
     * @param root The config object to retrieve the value from.
     * @param dottedString The dot-separated string representing a configuration option.
     * @return The required configuration value, or {@code null} if the setting doesn't exist.
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    protected static getConfigOption(Map root, String dottedString) {
        def parts = dottedString.split(NAME_SEPARATOR_REGEX)
        def firstParts = parts[0..<(parts.size() - 1)]
        def configEntry = firstParts.inject(root) { Map config, String keyPart ->
            config?.get(keyPart)
        }

        return configEntry?.get(parts[-1])
    }

    protected static getConfigOptionAsList(ConfigObject root, String dottedString) {
        def currentValue = getConfigOption(root, dottedString)
        currentValue = currentValue != null ? new ArrayList(currentValue) : []

        setConfigOption(root, dottedString, currentValue)
        return currentValue
    }

    /**
     * <p>Takes a dot-separated string, such as "test.report.dir", and sets the corresponding
     * config object property, {@code root.test.report.dir}, to the given value.</p>
     * <p><em>Note</em> the {@code @CompileDynamic} annotation is currently required due to
     * issue <a href="https://jira.codehaus.org/browse/GROOVY-6480">GROOVY-6480</a>.</p>
     * @param root The config object to set the value on.
     * @param dottedString The dot-separated string representing a configuration option.
     * @param value The new value for this option.
     * @return The map containing the final part of the dot-separated string as a
     * key. In other words, {@code retval.dir == value} for the dotted string example above.
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    protected static Map setConfigOption(ConfigObject root, String dottedString, value) {
        def parts = dottedString.split(NAME_SEPARATOR_REGEX)
        def firstParts = parts[0..<(parts.size() - 1)]
        def configEntry = firstParts.inject(root) { ConfigObject config, String keyPart ->
            config.getProperty(keyPart)
        }

        configEntry.setProperty(parts[-1], value)
        return configEntry
    }

    @SuppressWarnings("DuplicateNumberLiteral")
    protected static void clearConfigOption(ConfigObject root, String dottedString) {
        def parts = dottedString.split(NAME_SEPARATOR_REGEX)
        def currentConfig = root
        def configParts = []
        for (part in parts) {
            configParts << currentConfig
            currentConfig = currentConfig.getProperty(part)
        }

        configParts[-1].remove(parts[-1])
        if (parts.size() == 1) return

        for (int i in 2..parts.size()) {
            if (configParts[-i][parts[-i]]) break
            configParts[-i].remove(parts[-i])
        }
    }

    protected static ConfigObject loadDefaultConfig() {
        def cls = this
        return new ConfigSlurper().parse(cls.getResource("defaultConfig.groovy").text)
    }

    protected static Map loadConfigFromSystemProperties(overrideConfig) {
        return System.properties.findAll {
            it.key.startsWith(SYSPROP_OVERRIDE_PREFIX)
        }.each { String key, String value ->
            def settingName = key[SYSPROP_OVERRIDE_PREFIX.size()..-1]

            if (!validateSetting(settingName, VALID_OPTIONS, value)) {
                log.warning "Unknown option '$settingName' or its values are invalid: ${value}"
            }

            setConfigOption overrideConfig, settingName, value
        }
    }

    protected static File getJsonConfigFile(File userConfigFile) {
        return new File(userConfigFile.parentFile, JSON_CONFIG_FILENAME)
    }

    /**
     * Adds the data from a map to a given Groovy configuration object. This
     * works recursively, so any values in the source map that are maps themselves
     * are treated as a set of sub-keys in the configuration.
     */
    protected static void addConfigEntries(Map data, ConfigObject obj) {
        data?.each { key, value ->
            if (value instanceof Map) {
                addConfigEntries(value, obj.getProperty(key))
            }
            else obj.setProperty(key, value)
        }
    }

    /**
     * <p>Takes two maps and works out which keys are shared between two maps.
     * Crucially, this method recurses such that it checks the keys of any
     * nested maps as well. For example, if two maps have the same keys and
     * sub-keys such as [one: [two: "test"]], then the key "one.two" is
     * considered to be a shared key. Note how the keys of sub-maps are
     * referemced using dot ('.') notation.</p>
     * @param map1 The first map.
     * @param map2 The map to compare against the first map.
     * @return A list of the keys that both maps share. If either map is empty
     * or the two share no keys at all, this method returns an empty list.
     * Sub-keys are referenced using dot notation ("my.option.override") in the
     * same way that <tt>ConfigObject</tt> keys are converted when the settings
     * are flattened.
     */
    protected static List findIntersectKeys(Map map1, Map map2) {
        def keys = map1.keySet().intersect(map2.keySet())
        def result = new ArrayList(keys)

        for (k in keys) {
            if (map1[k] instanceof Map && map2[k] instanceof Map) {
                result.remove k
                result += findIntersectKeys(map1[k], map2[k]).collect { k + NAME_SEPARATOR + it }
            }
        }

        return result
    }

    @SuppressWarnings("DuplicateStringLiteral")
    private static class ProxyAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (requestorType == RequestorType.PROXY) {
                def prot = requestingProtocol.toLowerCase()
                def (host, port, user, password) = ["Host", "Port", "User", "Password"].collect { prop ->
                    System.getProperty(prot + ".proxy${prop}", prop == "Port" ? "80" : "")
                }

                if (requestingHost.equalsIgnoreCase(host) && port.toInteger() == requestingPort) {
                    return new PasswordAuthentication(user, password.toCharArray())
                }
            }

            return null
        }
    }
}
