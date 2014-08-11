package uk.co.cacoethes.lazybones.config;

/**
 * Represents a problem with the value of a Lazybones configuration setting.
 * For example, this is often thrown when the value type does not match the
 * configured type of the setting.
 */
public class InvalidSettingException extends RuntimeException {
    private String settingName;
    private Object value;

    public InvalidSettingException(String settingName, Object value) {
        this(settingName, value, getDefaultMessage(settingName, value));
    }

    public InvalidSettingException(String settingName, Object value, String message) {
        super(message);
        this.settingName = settingName;
        this.value = value;
    }

    public String getSettingName() { return this.settingName; }
    public Object getValue() { return this.value; }

    private static String getDefaultMessage(final String settingName, final Object value) {
        return "The value '" + value + "' for configuration setting '" + settingName + "' is invalid";
    }
}
