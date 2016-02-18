package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 11/04/2014.
 */
interface PackageCache {
    File copyToCache(URI packageUrl, boolean overwrite)
    File copyToCache(String repoName, String packageName, String packageVersion, URI packageUrl, boolean overwrite)
    File getPackage(URI packageUrl)
    File getPackage(String repoName, String name, String version)
    CachedPackage getPackageInfo(URI packageUrl)
    CachedPackage getPackageInfo(String repoName, String name, String version)
    boolean hasPackage(URI packageUrl)
    boolean hasPackage(String repoName, String name, String version)
    List<CachedPackage> listPackages()
}
