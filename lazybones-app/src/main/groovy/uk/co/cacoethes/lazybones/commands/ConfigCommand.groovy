package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import joptsimple.OptionParser
import joptsimple.OptionSet
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.lazybones.config.InvalidSettingException
import uk.co.cacoethes.lazybones.config.UnknownSettingException

/**
 * <p>This Lazybones command allows users to interact with the configuration settings
 * via the command line. To do this, it works with a managed JSON configuration
 * file and has knowledge of what settings are supported by the application. It's
 * important to understand that any settings in the standard user configuration
 * file (~/.lazybones/config.groovy) take precedence over those in the JSON config.
 * Fortunately the `set` and `add` sub-commands warn the user when they try to
 * modify a setting that is overridden in this way.<p>
 */
@Log
class ConfigCommand extends AbstractCommand {
    static final String USAGE = """\
USAGE: config set <option> <value> [<value> ...]
       config add <option> <value>
       config clear <option>
       config show [--all] <option>
       config list

  where  set    = Changes or initialises the value of a named setting.
         add    = Adds a new value to an array setting.
         clear  = Removes a named setting such that its default value will be
                  used by Lazybones.
         show   = Displays the current value of a named setting.
         list   = Displays all available configuration settings and their
                  types.
         option = The name of the configuration option to modify.
         value  = The configuration option's new value. Multiple values are
                  treated as an array.
"""

    private static final String INDENT = "    "
    private static final String ALL_OPT = "all"
    private static final String INCORRECT_ARG_COUNT_MSG = "Incorrect number of arguments for config "
    private static final String OVERRIDE_WARNING_MSG = "The user configuration file overrides this setting, so " +
            "the new value won't take effect"

    Configuration config

    ConfigCommand(Configuration config) { this.config = config }

    @Override
    String getName() { return "config" }

    @Override
    String getDescription() {
        return "Displays general help, or help for a specific command."
    }

    @Override
    protected IntRange getParameterRange() {
        // There is unfortunately no way to set an argument limit on a sub-command
        // basis, so we just use a suitable range for the 'config set' command. The
        // doExecute() method performs a secondary check.
        return 1..Integer.MAX_VALUE
    }

    @Override
    protected OptionParser doAddToParser(OptionParser parser) {
        parser.accepts(ALL_OPT, "Used with `show` to display all setting values.")
        return parser
    }

    @Override
    protected String getUsage() { return USAGE }

    @Override
    @SuppressWarnings("DuplicateNumberLiteral")
    protected int doExecute(OptionSet cmdOptions, Map globalOptions, Configuration config) {
        def cmdArgs = cmdOptions.nonOptionArguments()
        switch (cmdArgs[0]) {
        case "set":
            if (cmdArgs.size() < 3) {
                log.severe getHelp(INCORRECT_ARG_COUNT_MSG + cmdArgs[0])
                return 1
            }
            return configSet(cmdOptions)

        case "add":
            if (cmdArgs.size() < 3 || cmdArgs.size() > 4) {
                log.severe getHelp(INCORRECT_ARG_COUNT_MSG + cmdArgs[0])
                return 1
            }
            return configAdd(cmdOptions)

        case "clear":
            if (cmdArgs.size() < 2 || cmdArgs.size() > 3) {
                log.severe getHelp(INCORRECT_ARG_COUNT_MSG + cmdArgs[0])
                return 1
            }
            return configClear(cmdOptions)

        case "show":
            if (cmdArgs.size() > 2 || (cmdArgs.size() == 1 && !cmdOptions.has(ALL_OPT))) {
                log.severe getHelp(INCORRECT_ARG_COUNT_MSG + cmdArgs[0])
                return 1
            }
            return cmdOptions.has(ALL_OPT) ? configShowAll() : configShow(cmdOptions)

        case "list":
            if (cmdArgs.size() > 1) {
                log.severe getHelp(INCORRECT_ARG_COUNT_MSG + cmdArgs[0])
                return 1
            }
            return configList()

        default:
            log.severe getHelp("Invalid config command: '${cmdArgs[0]}'")
            return 1
        }
    }

    /**
     * <p>Implements the `config set` command. This updates (or initialises) the
     * value of the given setting in the managed JSON config file. It also
     * performs a check on whether the setting is a known one. If not, an error
     * message is displayed instead.</p>
     * <p>In the case that the user config already specifies a value for the
     * setting, the value is still written to the JSON file, but a warning is
     * also displayed because the user config value takes precedence.</p>
     * @param cmdOptions The command options from JOptSimple, including the
     * `set` as the first non-option argument.
     * @return An exit code. 0 means the command successfully completed, while
     * any other value indicates failure.
     */
    protected int configSet(OptionSet cmdOptions) {
        def cmdArgs = cmdOptions.nonOptionArguments()
        def config = Configuration.initConfiguration()

        try {
            if (!config.putSetting(cmdArgs[1], cmdArgs[2..-1].join(", "))) {
                log.warning OVERRIDE_WARNING_MSG
            }
            config.storeSettings()
            return 0
        }
        catch (UnknownSettingException ex) {
            log.severe "Unrecognized setting: '${ex.settingName}'"
            return 1
        }
        catch (InvalidSettingException ex) {
            log.severe ex.message
            return 1
        }
    }

    /**
     * <p>Implements the `config add` command. This adds a value to the given
     * setting, under the assumption that it is an array. If the setting is not
     * configured as an array, this command will fail. If the setting has no
     * value(s) associated with it, this command initialises it to an array
     * containing just the given value.</p>
     * <p>The setting is updated in the managed JSON config file only. The
     * command also performs a check on whether the setting is a known one. If
     * not, an error message is displayed instead.</p>
     * <p>In the case that the user config already specifies a value for the
     * setting, the value is still written to the JSON file, but a warning is
     * also displayed because the user config value takes precedence.</p>
     * @param cmdOptions The command options from JOptSimple, including the
     * `add` as the first non-option argument.
     * @return An exit code. 0 means the command successfully completed, while
     * any other value indicates failure.
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    protected int configAdd(OptionSet cmdOptions) {
        def cmdArgs = cmdOptions.nonOptionArguments()
        def config = Configuration.initConfiguration()

        try {
            if (!config.appendToSetting(cmdArgs[1], cmdArgs[2])) {
                log.warning OVERRIDE_WARNING_MSG
            }
            config.storeSettings()
            return 0
        }
        catch (UnknownSettingException ex) {
            log.severe "Unrecognized setting: '${ex.settingName}'"
            return 1
        }
        catch (InvalidSettingException ex) {
            log.severe ex.message
            return 1
        }
    }

    /**
     * <p>Implements the `config clear` command. This removes the given setting
     * from the managed JSON config file. The command also performs a check on
     * whether the setting is a known one. If not, an error message is displayed
     * instead.</p>
     * @param cmdOptions The command options from JOptSimple, including the
     * `clear` as the first non-option argument.
     * @return An exit code. 0 means the command successfully completed, while
     * any other value indicates failure.
     */
    protected int configClear(OptionSet cmdOptions) {
        def cmdArgs = cmdOptions.nonOptionArguments()
        def config = Configuration.initConfiguration()

        try {
            config.clearSetting(cmdArgs[1])
            config.storeSettings()
            return 0
        }
        catch (UnknownSettingException ex) {
            log.severe "Unrecognized setting: '${ex.settingName}'"
            return 1
        }
    }

    /**
     * <p>Implements the `config show` command, displaying the current value
     * for the given setting or the entire current configuration if the
     * <tt>--all</tt> option is used.</p>
     * @param optionSet The command options from JOptSimple, including the
     * `show` as the first non-option argument.
     * @return An exit code. 0 means the command successfully completed, while
     * any other value indicates failure.
     */
    int configShow(OptionSet cmdOptions) {
        def cmdArgs = cmdOptions.nonOptionArguments()
        def config = Configuration.initConfiguration()

        try {
            println config.getSetting(cmdArgs[1])
            return 0
        }
        catch (UnknownSettingException ex) {
            log.severe "Unrecognized setting: '${ex.settingName}'"
            return 1
        }
    }

    @SuppressWarnings("DuplicateNumberLiteral")
    int configShowAll() {
        def settings = Configuration.initConfiguration().allSettings
        final columnWidth = settings.keySet().inject(0) {
            int max, String key -> Math.max(key.size(), max)
        } + 3

        println "Current configuration settings:"
        println()
        for (Map.Entry setting in settings) {
            println INDENT + setting.key.padRight(columnWidth) + "= " + setting.value
        }

        return 0
    }

    /**
     * <p>Implements the `config list` command, displaying all the available
     * configuration settings supported by Lazybones.</p>
     * @param optionSet The command options from JOptSimple, including the
     * `list` as the first non-option argument.
     * @return An exit code. 0 means the command successfully completed, while
     * any other value indicates failure.
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    int configList() {
        def validSettings = Configuration.VALID_OPTIONS
        final columnWidth = validSettings.keySet().inject(0) {
            int max, String key -> Math.max(key.size(), max)
        } + 3

        println "Valid Lazybones configuration settings:"
        println()
        for (Map.Entry setting in validSettings) {
            println INDENT + setting.key.padRight(columnWidth) + setting.value.simpleName
        }

        return 0
    }
}
