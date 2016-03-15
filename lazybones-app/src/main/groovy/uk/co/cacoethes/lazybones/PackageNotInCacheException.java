package uk.co.cacoethes.lazybones;

import uk.co.cacoethes.lazybones.api.PackageIdentifier;

import java.net.URI;

/**
 * Thrown when a Lazybones template package can't be found in the cache or in
 * a remote repository. It may result from the lack of any package with the
 * required name or from the lack of a specific version of that package. In
 * the former case, the {@link #version} property will be {@code null}.
 */
public class PackageNotInCacheException extends RuntimeException {
    private PackageIdentifier packageId;
    private URI packageUri;
//
//    public PackageNotInCacheException(String packageName) {
//        super(getDefaultMessage(packageName));
//        this.name = packageName;
//    }
//
//    public PackageNotInCacheException(String packageName, String version) {
//        super(getDefaultMessage(packageName));
//        this.name = packageName;
//        this.version = version;
//    }
//
//    public PackageNotInCacheException(String packageName, Throwable cause) {
//        super(getDefaultMessage(packageName), cause);
//        this.name = packageName;
//    }
//
//    public PackageNotInCacheException(String packageName, String version, Throwable cause) {
//        super(getDefaultMessage(packageName), cause);
//        this.name = packageName;
//        this.version = version;
//    }
//
//    public String getName() { return name; }
//    public String getVersion() { return version; }
//
//    private static String getDefaultMessage() {
//        return "No package found in cache with name '" + packageName + "'";
//    }
}
