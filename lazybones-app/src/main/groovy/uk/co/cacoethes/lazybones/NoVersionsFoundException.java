package uk.co.cacoethes.lazybones;

/**
 * Thrown when a Lazybones template package doesn't have any versions associated
 * with it. This can happen if the package is defined in a repository but no
 * actual packages have been published.
 */
public class NoVersionsFoundException extends RuntimeException {
    private String packageName;

    public NoVersionsFoundException(String packageName) {
        super(getDefaultMessage(packageName));
        this.packageName = packageName;
    }

    public NoVersionsFoundException(String packageName, Throwable cause) {
        super(getDefaultMessage(packageName), cause);
        this.packageName = packageName;
    }

    public String getPackageName() { return packageName; }

    private static String getDefaultMessage(final String packageName) {
        return "No versions of template '" + packageName + "' found";
    }
}
