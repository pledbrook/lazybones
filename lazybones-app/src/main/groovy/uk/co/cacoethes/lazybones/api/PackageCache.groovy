package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 11/04/2014.
 */
interface PackageCache {
    File copyToCache(URI packageUrl)
    File copyToCache(String packageName, String packageVersion, URI packageUrl)
    File getPackage(URI packageUrl)
    File getPackage(String name, String version)
    boolean hasPackage(URI packageUrl)
    boolean hasPackage(String name, String version)
}
