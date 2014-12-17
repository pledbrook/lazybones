package uk.co.cacoethes.lazybones.config

/**
 * Represents a problem with the value of a Lazybones configuration setting.
 * For example, this is often thrown when the value type does not match the
 * configured type of the setting.
 */
class MultipleInvalidSettingsException extends RuntimeException {
    private final List<String> settingNames

    MultipleInvalidSettingsException(List<String> settingNames) {
        this(settingNames, getDefaultMessage(settingNames))
    }

    MultipleInvalidSettingsException(List<String> settingNames, String message) {
        super(message)
        this.settingNames = new ArrayList(settingNames)
    }

    List<String> getSettingNames() { return this.settingNames }

    private static String getDefaultMessage(final List<String> settingNames) {
        return "The following configuration settings are invalid: " + settingNames.join(", ")
    }
}
