package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 11/04/2014.
 */
interface PackageCache {
    File copyToCache(URI packageUrl, boolean overwrite)
    File copyToCache(PackageIdentifier pkgId, URI packageUrl, boolean overwrite)
    File getPackage(URI packageUrl)
    File getPackage(PackageIdentifier pkgId)
    CachedPackage getPackageInfo(URI packageUrl)
    CachedPackage getPackageInfo(PackageIdentifier pkgId)
    CachedPackage findPackageInfo(String name)
    CachedPackage findPackageInfo(String name, String version)
    boolean hasPackage(URI packageUrl)
    boolean hasPackage(PackageIdentifier pkgId)
    List<CachedPackage> listPackages()
}
