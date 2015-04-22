package uk.co.cacoethes.lazybones.config

import spock.lang.Shared
import spock.lang.Specification

import spock.lang.Unroll

/**
 * Created by pledbrook on 07/08/2014.
 */
class ConfigurationSpec extends Specification {
    private static final String BASE_CONFIG_VALUE = "http://wwww.nowhere.net/repo/"

    @Shared Map knownSettings = [
            "stuff": String,
            "num.option": Integer,
            "test.option": Integer,
            "other.option": String,
            "new.option": String,
            "repo.main.url": String,
            "options.logLevel": String,
            "options.quiet": Boolean,
            "mappings.*": String,
            "mvn.repo.*": String,
            "array.option": String[],
            "array.override": String[],
            "array.num.bers": Integer[]]

    @Unroll
    def "Getting known #type setting returns its value"() {
        given: "An initialised configuration"
        def config = initConfig()

        expect: "I retrieve an existing setting's current value"
        config.getSetting(optionName) == expectedValue

        where:
        optionName      |   expectedValue      |    type
        "test.option"   |         -1           |  "managed"
        "other.option"  |       "stuff"        |  "override"
        "repo.main.url" |   BASE_CONFIG_VALUE  |   "base"
        "array.option"  |   ["1", "2", "3"]    |   "managed"
    }

    def "Getting unknown setting throws exception"() {
        given: "An initialised configuration"
        def config = initConfig()

        when: "I retrieve an unknown setting"
        config.getSetting("test.unknown")

        then: "An exception is thrown"
        UnknownSettingException ex = thrown()
        ex.message == "The configuration setting 'test.unknown' is not recognized"
    }

    def "Getting overridden setting"() {
        given: "An initialised configuration"
        def config = initConfig(other: [option: "initial"])

        expect: "I retrieve the override value for a setting"
        config.getSetting("other.option") == "initial"
    }

    @Unroll
    def "Fetching sub-settings as a map"() {
        given: "An initial configuration containing sub-keys"
        def config = initConfig()

        when: "I fetch a particular key"
        def result = config.getSubSettings(rootSetting)

        then: "All the sub-keys and their values are returned"
        result == expected

        where:
        rootSetting    |              expected
        "options"      |    [logLevel: "debug", quiet: true]
        "mappings"     |  [dev: "url-one", custom: "url-two"]
        "mvn"          | [repo: [dev: "url-3", other: "url-4"]]
        "mvn.repo"     |    [dev: "url-3", other: "url-4"]
        "mvn.repo.br"  |                [:]
    }

    @Unroll
    def "Fetching sub-settings that don't exist or aren't sub-settings"() {
        given: "An initial configuration containing sub-keys"
        def config = initConfig()

        when: "I fetch a particular key"
        config.getSubSettings(rootSetting)

        then: "All the sub-keys and their values are returned"
        thrown(expected)

        where:
         rootSetting    |           expected
          "unknown"     |   UnknownSettingException
        "mvn.not.known" |   UnknownSettingException
        "test.option"   |   InvalidSettingException
        "options.quiet" |   InvalidSettingException
    }

    def "Fetching current settings as a flat property map"() {
        given: "An initial configuration"
        def config = initConfig(stuff: "test", other: [option: "override"])

        when: "I fetch all settings"
        def result = config.getAllSettings()

        then: "All the keys are in dot-separated form"
        result["stuff"] == "test"
        result["test.option"] == -1
        result["other.option"] == "override"
        result["mvn.repo.dev"] == "url-3"
        result["array.option"] == ["1", "2", "3"]
        !result["unknown"]
    }

    @Unroll
    def "Setting options"() {
        given: "An initialised configuration"
        def config = initConfig()
        assert config.getSetting("new.option") == null

        when: "I change the value of a known setting"
        def retval = config.putSetting(settingName, newValue)

        then: "the new value is available and type conversion from string occurs if necessary"
        retval == !overridden
        config.getSetting(settingName) == expectedValue

        where:
        settingName   |    newValue    |  overridden  |  expectedValue
        "test.option"  |      100       |    true      |      100
        "test.option"  |     "220"      |    true      |      220
        "new.option"   |  "irrelevant"  |    false     |  "irrelevant"
        "other.option" |    "stuff"     |    false     |    "stuff"
        "other.option" |    "stuff"     |    false     |    "stuff"
        "array.option" |["test", "list"]|    false     |["test", "list"]
    }

    @Unroll
    def "Adding or updating invalid settings"() {
        given: "An initialised configuration"
        def config = initConfig()
        assert config.getSetting("new.option") == null

        when: "I add or change an invalid setting (unknown or invalid value)"
        config.putSetting(settingName, newValue)

        then: "I get the appropriate exception"
        thrown(expected)

        where:
        settingName   |    newValue    |         expected
         "unknown"    |      100       |  UnknownSettingException
        "not.known"   |     "test"     |  UnknownSettingException
        "test.option" |   new Date()   |  InvalidSettingException
        "test.option" | "not a number" |  InvalidSettingException
        "array.option"|  ["test", 1]   |  InvalidSettingException
    }

    @Unroll
    def "Adding values to a list (multi-value) setting"() {
        given: "An initialised configuration"
        def config = initConfig(array: [override: ["apple"]])

        when: "I change the value of a known setting"
        def retval = config.appendToSetting(settingName, newValue)

        then: "the new value is available and type conversion from string occurs if necessary"
        retval == !overridden
        config.getSetting(settingName) == expectedValue

        where:
          settingName    |  newValue    |  overridden  |   expectedValue
        "array.option"   |    "4"       |    false     | ["1", "2", "3", "4"]
        "array.override" |   "pear"     |    true      |  ["apple", "pear"]
        "array.num.bers" |     64       |    false     |       [64]
    }

    @Unroll
    def "Adding invalid values to multi-value and non multi-value settings"() {
        given: "An initialised configuration"
        def config = initConfig(array: [override: ["apple"]])

        when: "I add or change an invalid setting (unknown or invalid value)"
        config.appendToSetting(settingName, newValue)

        then: "I get the appropriate exception"
        thrown(expected)

        where:
        settingName   |    newValue    |         expected
         "unknown"    |      100       |  UnknownSettingException
        "not.known"   |     "test"     |  UnknownSettingException
        "test.option" |      100       |  InvalidSettingException
        "array.option"|   new Date()   |  InvalidSettingException
    }

    @Unroll
    def "Clearing a setting"() {
        given: "An initialised configuration"
        def config = initConfig(array: [override: ["apple"]])

        when: "I clear the values of a known setting"
        config.clearSetting(settingName)

        then: "the setting becomes an empty list"
        config.getSetting(settingName) == null

        where:
        settingName << ["stuff", "test.option", "mvn.repo.dev", "array.option", "array.override", "array.num.bers"]
    }

    @Unroll
    def "Clearing values from unknown settings"() {
        given: "An initialised configuration"
        def config = initConfig(array: [override: ["apple"]])

        when: "I add or change an invalid setting (unknown or invalid value)"
        config.clearSetting(settingName)

        then: "I get the appropriate exception"
        thrown(expected)

        where:
        settingName   |          expected
        "unknown"     |   UnknownSettingException
        "not.known"   |   UnknownSettingException
    }

    @Unroll
    def "Validate settings and their values based on known option names and types"() {
        when: "I validate a setting and its value"
        def result = Configuration.validateSetting(settingName, knownSettings, settingValue)

        then: "A list of those values that are invalid is returned"
        result == expected

        where:
          settingName      |      settingValue      |     expected
            "stuff"        |        "ilike"         |       true
         "test.option"     |          10            |       true
         "other.option"    |        "test"          |       true
         "mappings.dev"    |     "venividivici"     |       true
        "mappings.test"    |          100           |       false
        "mappings.dev.one" |        "working"       |       true
          "num.option"     |     "not a number"     |       false
         "mvn.repo.dev"    |       "some-url"       |       true
         "array.option"    | ["abc", "def", "ghi"]  |       true
         "array.num.bers"  |       [1, 5, 7]        |       true
         "array.num.bers"  |     [1, "five", 7]     |       false
    }

    @Unroll
    def "Handle unknown and non-leaf settings during validation (#settingName)"() {
        when: "I validate an unknown or non-leaf setting"
        def result = Configuration.validateSetting(settingName, knownSettings, settingValue)

        then: "The appropriate exception is thrown"
        thrown(expected)

        where:
          settingName         |      settingValue      |        expected
           "unknown"          |         "abc"          | UnknownSettingException
         "options.loud"       |         "abc"          | UnknownSettingException
           "mappings"         |[dev: "one", test: 100] | UnknownSettingException
           "mvn.repo"         |    [dev: "some-url"]   | UnknownSettingException
             "mvn"            |  [repo: [dev: "url"]]  | UnknownSettingException
        "one.two.three"       |       "unknown"        | UnknownSettingException
    }

    def "Constructor verifies initial settings"() {
        when: "I initialise a Configuration with invalid data"
        initConfig([num: [option: "not a number"]])

        then: "An exception is thrown"
        MultipleInvalidSettingsException ex = thrown()
        ex.message == "The following configuration settings are invalid: num.option"
    }

    @Unroll
    def "Getting a config option"() {
        given: "An initial configuration"
        def config = new ConfigObject()
        config.test.option = "irrelevant"
        config.other.option = "irrelevant other"

        when: "I retrieve a dotted notation setting"
        def val = Configuration.getConfigOption(config, settingName)

        then: "I get the expected value back, or null if it doesn't exist"
        val == expected

        where:
           settingName   |    expected
          "test.option"  |  "irrelevant"
          "test.other"   |     null
        "not.known.here" |     null
    }

    @Unroll
    def "Setting a config option"() {
        given: "An initial configuration"
        def config = new ConfigObject()
        config.test.option = "irrelevant"
        config.other.option = "irrelevant other"

        when: "I retrieve a dotted notation setting"
        def val = Configuration.setConfigOption(config, settingName, newValue)

        then: "I get the expected value back, or null if it doesn't exist"
        val.containsValue(newValue)
        Configuration.getConfigOption(config, settingName) == newValue

        where:
        settingName   |     newValue
        "test.option"  |  "I've changed"
        "test.other"   |  "irrelevant 2"
        "not.known.here"|  "irrelevant 3"
    }

    def "Dynamically add configuration settings to a ConfigObject instance"() {
        given: "A fresh ConfigObject"
        def config = new ConfigObject()

        when: "I add..."
        Configuration.addConfigEntries(
                [test: [option: "123", num: 5], other: [custom: [setting: "test"]]],
                config)

        then: "The shared keys are returned as a list"
        config.test.option == "123"
        config.test.num == 5
        config.other.custom.setting == "test"
    }

    @Shared map1 = [one: 1, my: [option: [override: "test"], other: 1.1], key: "A", an: [goat: 300]]
    @Shared map2 = [two: 2, my: [option: [override: "irrelevant"], sense: "smell"], key: "A", an: [ape: 400]]

    @Unroll
    def "Find hierarchical keys shared between two distinct maps"() {
        when: "The keys of the two maps are compared"
        def result = Configuration.findIntersectKeys(leftMap, rightMap)

        then: "The shared keys are returned as a list"
        result.sort() == expected

        where:
        leftMap   |  rightMap  |  expected
        [:]     |     [:]    |     []
        map1    |     [:]    |     []
        [:]     |    map2    |     []
        map1    |    map2    | ["key", "my.option.override"]
        map1    |    map1    | ["an.goat", "key", "my.option.override", "my.other", "one"]
    }

    private Configuration initConfig(Map additionalOverrides = [:]) {
        def baseConfig = new ConfigObject()
        baseConfig.repo.main.url = BASE_CONFIG_VALUE
        return new Configuration(
                baseConfig,
                [test: [option: -1]] + additionalOverrides,
                [other: [option: "stuff"],
                 options: [logLevel: "debug", quiet: true],
                 mappings: [dev: "url-one", custom: "url-two"],
                 mvn: [repo: [dev: "url-3", other: "url-4"]],
                 array: [option: ["1", "2", "3"], override: ["a", "b"]]],
                knownSettings,
                new File("config.json"))
    }
}
