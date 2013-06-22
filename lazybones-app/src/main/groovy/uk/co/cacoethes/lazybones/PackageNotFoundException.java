package uk.co.cacoethes.lazybones;

/**
 * Thrown when a Lazybones template package can't be found in the cache or in
 * a remote repository. It may result from the lack of any package with the
 * required name or from the lack of a specific version of that package. In
 * the former case, the {@link #version} property will be {@code null}.
 */
public class PackageNotFoundException extends RuntimeException {
    private String name;
    private String version;

    public PackageNotFoundException(String packageName) {
        super(getDefaultMessage(packageName));
        this.name = packageName;
    }

    public PackageNotFoundException(String packageName, String version) {
        super(getDefaultMessage(packageName));
        this.name = packageName;
        this.version = version;
    }

    public PackageNotFoundException(String packageName, Throwable cause) {
        super(getDefaultMessage(packageName), cause);
        this.name = packageName;
    }

    public PackageNotFoundException(String packageName, String version, Throwable cause) {
        super(getDefaultMessage(packageName), cause);
        this.name = packageName;
        this.version = version;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }

    private static String getDefaultMessage(final String packageName) {
        return "No template found with name '" + packageName + "'";
    }
}
