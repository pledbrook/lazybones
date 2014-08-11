package uk.co.cacoethes.lazybones.config;

/**
 * Created by pledbrook on 08/08/2014.
 */
public class UnknownSettingException extends RuntimeException {
    private String settingName;

    public UnknownSettingException(String settingName) {
        super(getDefaultMessage(settingName));
        this.settingName = settingName;
    }

    public UnknownSettingException(Throwable cause) { super(cause); }

    public UnknownSettingException(String settingName, Throwable cause) {
        super(getDefaultMessage(settingName), cause);
        this.settingName = settingName;
    }

    public String getSettingName() { return this.settingName; }

    private static String getDefaultMessage(final String settingName) {
        return "The configuration setting '" + settingName + "' is not recognized";
    }
}
