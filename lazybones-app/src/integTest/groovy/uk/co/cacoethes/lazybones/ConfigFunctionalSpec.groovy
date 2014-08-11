package uk.co.cacoethes.lazybones

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import spock.lang.Unroll

/**
 * Functional tests for the <tt>config</tt> command, covering the writing and
 * reading of config.json and testing whether the user config file can override
 * the settings in config.json.
 */
class ConfigFunctionalSpec extends AbstractFunctionalSpec {

    def jsonConfig
    def userConfig

    void setup() {
        new File(jsonConfigFilePath).delete()

        jsonConfig = loadJsonConfig()
        userConfig = loadUserConfig()
    }

    void cleanup() {
        println "Final JSON config content:"
        println "-------------------------"
        println new JsonBuilder(loadJsonConfig()).toPrettyString()
        new File(jsonConfigFilePath).delete()
    }

    def "Setting a config option writes it to config.json"() {
        given: "The JSON config file does not already contain the setting"
        assert jsonConfig.test?.my?.option == null

        when: "I run the config set command with that setting"
        def exitCode = runCommand(["config", "set", "test.my.option", "100"], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The setting is now in the JSON config file"
        loadJsonConfig().test.my.option == 100
        !(output =~ "Exception")
    }

    def "Setting a config option that already exists in user config file"() {
        given: "The JSON config file does not already contain the setting"
        assert jsonConfig.test?.option?.override == null

        and: "the user config file does contain it"
        assert userConfig.test?.option?.override != null

        when: "I run the config set command with that setting"
        def exitCode = runCommand(["config", "set", "test.option.override", "new value"], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The setting is now in the JSON config file"
        loadJsonConfig().test.option.override == "new value"

        and: "A warning is printed"
        output =~ "The user configuration file overrides this setting, so the new value won't take effect"
        !(output =~ "Exception")
    }

    @Unroll
    def "Setting a config option to an array"() {
        given: "The JSON config file does not already contain the setting"
        if (inputValue == ["one"]) {
            assert jsonConfig.test?.option?.array == null
        }

        when: "I run the config set command with that setting"
        def exitCode = runCommand(["config", "set", settingName, *inputValue], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The setting is now in the JSON config file"
        evaluateJsonSetting(settingName) == settingValue

        and: "There is no exception"
        !(output =~ "Exception")

        where:
        settingName            |          inputValue         |       settingValue
        "test.option.array"    |           ["one"]           |         ["one"]
        "test.option.array"    | ["test", "array", "stuff"]  | ["test", "array", "stuff"]
        "test.integer.array"   |           ["120"]           |         [120]
        "test.integer.array"   |       ["5", "4", "3"]       |       [5, 4, 3]
    }

    @Unroll
    def "Using 'config #command' with an unknown setting displays an error"() {
        when: "I run any config command with an invalid/unknown setting"
        def exitCode = runCommand(["config", command, "unknown.option"] + extraArgs, baseWorkDir)

        then: "The exit code is non-zero"
        exitCode == 1

        and: "An error message is displayed"
        output =~ /Unrecognized setting: 'unknown.option'/

        and: "There is no exception"
        !(output =~ "Exception")

        where:
        command << ["set", "add", "clear", "show"]
        extraArgs << ["irrelevant", "irrelevant", [], []]
    }

    @Unroll
    def "Adding #newValue to an array config option"() {
        given: "An existing JSON config file does not already contain the setting"
        generateJsonConfigFile(test: [adding: [array: ["hello"]]])

        when: "I run the config set command with that setting"
        def exitCode = runCommand(["config", "add", settingName, newValue], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The setting is now in the JSON config file"
        evaluateJsonSetting(settingName) == resultingSetting

        and: "There is no exception"
        !(output =~ "Exception")

        where:
             settingName      |     newValue    |   resultingSetting
        "test.integer.array"  |      "100"      |        [100]
        "test.adding.array"   |     "world"     |  ["hello", "world"]
    }

    def "Adding to a non-array setting displays an error"() {
        when: "I run the config set command with that setting"
        def exitCode = runCommand(["config", "add", "test.my.option", "irrelevant"], baseWorkDir)

        then: "The exit code indicates failure"
        exitCode == 1

        and: "The setting is not in the JSON config file"
        loadJsonConfig().test?.my?.option == null

        and: "An error is reported but no exception"
        output =~ /Setting 'test.my.option' is not an array type, so you cannot add to it/
        !(output =~ "Exception")
    }

    def "Assigning a value of the incorrect type to a setting"() {
        when: "I run the config set command with that setting"
        def exitCode = runCommand(["config", "set", "test.my.option", "irrelevant"], baseWorkDir)

        then: "The exit code indicates failure"
        exitCode == 1

        and: "The setting is not in the JSON config file"
        loadJsonConfig().test?.my?.option == null

        and: "An error is reported but no exception"
        output =~ /The value 'irrelevant' for configuration setting 'test.my.option' is invalid/
        !(output =~ "Exception")
    }

    def "Clearing a config option"() {
        given: "An existing setting in the JSON config file"
        generateJsonConfigFile(test: [other: [array: ["hello"]], my: [option: 25]])

        when: "I run the config clear command with that setting"
        def exitCode = runCommand(["config", "clear", "test.other.array"], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The setting is no longer in the JSON config file"
        def json = loadJsonConfig()
        json.test.other == null
        json.test.my.option == 25

        and: "There is no exception"
        !(output =~ "Exception")
    }

    def "Clearing a config option that isn't set"() {
        given: "The JSON config file does not yet contain a setting"
        assert jsonConfig.test?.other?.array == null

        when: "I run the config clear command with that setting"
        def exitCode = runCommand(["config", "clear", "test.other.array"], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The setting is still not in the JSON config file"
        def json = loadJsonConfig()
        json.test?.other?.array == null

        and: "There is no exception"
        !(output =~ "Exception")
    }

    @Unroll
    def "Show sub-command displays current value of a configuration setting"() {
        given: "An existing setting in the JSON config file"
        generateJsonConfigFile(test: [other: [array: ["hello"]], my: [option: 25]])

        when: "I run the config show command with a known setting"
        def exitCode = runCommand(["config", "show", settingName], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The output lists the available settings"
        output.trim() == expected

        and: "There is no exception"
        !(output =~ "Exception")

        where:
             settingName       |     expected
        "test.option.override" | "Just an option"
          "test.other.array"   |     "[hello]"
           "test.my.option"    |       "25"
    }

    def "Missing setting name with 'show' command"() {
        given: "An existing setting in the JSON config file"
        generateJsonConfigFile(test: [other: [array: ["hello"]], my: [option: 25]])

        when: "I run the config show command with a known setting"
        def exitCode = runCommand(["config", "show"], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 1

        and: "The output lists the available settings"
        output.trim() =~ "^Incorrect number of arguments for config show"

        and: "There is no exception"
        !(output =~ "Exception")
    }

    def "Show sub-command displays all configuration settings with --all"() {
        given: "An existing setting in the JSON config file"
        generateJsonConfigFile(test: [other: [array: ["hello"]], my: [option: 25]])

        when: "I run the config show command with the --all option"
        def exitCode = runCommand(["config", "show", "--all"], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The output lists current setting values"
        output =~ /test.option.override\s+=\s+Just an option/
        output =~ /test.other.array\s+=\s+\[hello\]/
        output =~ /test.my.option\s+=\s+25/

        and: "There is no exception"
        !(output =~ "Exception")
    }

    def "List sub-command displays known configuration settings, with types"() {
        when: "I run the config list command"
        def exitCode = runCommand(["config", "list"], baseWorkDir)

        then: "The exit code is zero"
        exitCode == 0

        and: "The output lists the available settings"
        output =~ /config.file\s+String/
        output =~ /cache.dir\s+String/
        output =~ /options.*\s+String/
        output =~ /bintrayRepositories\s+String\[\]/
        output =~ /templates.mappings.*\s+URI/

        and: "There is no exception"
        !(output =~ "Exception")
    }

    def "Config command displays usage when incorrect number of arguments are provided"() {
        when: "I run the config command without specifying a sub-command"
        def exitCode = runCommand(["config"], baseWorkDir)

        then: "It returns a non-zero exit code and displays an error message"
        exitCode == 1
        output =~ /Incorrect number of arguments/
        output =~ /USAGE:/
    }

    private loadJsonConfig() {
        def jsonFile = new File(jsonConfigFilePath)
        return jsonFile.exists() ? new JsonSlurper().parseText(jsonFile.text) : [:]
    }

    private loadUserConfig() {
        return new ConfigSlurper().parse(new File(configFilePath).toURI().toURL())
    }

    private evaluateJsonSetting(String setting) {
        return setting.split(/\./).inject(loadJsonConfig()) { config, prop -> config[prop] }
    }

    private void generateJsonConfigFile(Map settings) {
        new File(jsonConfigFilePath).text = new JsonBuilder(settings).toPrettyString()
        jsonConfig = loadJsonConfig()
    }
}
