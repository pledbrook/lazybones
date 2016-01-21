package uk.co.cacoethes.lazybones.impl

import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.api.PackageCache

/**
 * Created by pledbrook on 11/04/2014.
 */
@Log
class DefaultPackageCache implements PackageCache {
    File cacheDir

    DefaultPackageCache(File cacheDir) {
        assert cacheDir

        this.cacheDir = cacheDir

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    @Override
    File copyToCache(URI packageUrl) {
        return copyToCache(getPackage(packageUrl), packageUrl)
    }

    @Override
    File copyToCache(String packageName, String packageVersion, URI packageUrl) {
        return copyToCache(getPackage(packageName, packageVersion), packageUrl)
    }

    @Override
    File getPackage(URI packageUrl) {
        return new File(cacheDir, calculatePackagePath(packageUrl))
    }

    @Override
    File getPackage(String name, String version) {
        return new File(cacheDir, calculatePackagePath(name, version))
    }

    @Override
    boolean hasPackage(URI packageUrl) {
        return getPackage(packageUrl).exists()
    }

    @Override
    boolean hasPackage(String name, String version) {
        return getPackage(name, version).exists()
    }

    protected File copyToCache(File packageFile, URI packageUrl) {
        log.fine "Attempting to download ${packageUrl} into ${packageFile}"

        try {
            packageFile.withOutputStream { OutputStream out ->
                packageUrl.toURL().withInputStream { InputStream input ->
                    out << input
                }
            }
        }
        catch (FileNotFoundException ex) {
            packageFile.deleteOnExit()
            throw new PackageNotFoundException(packageName, packageVersion, ex)
        }
        catch (all) {
            packageFile.deleteOnExit()
            throw all
        }
        return null
    }

    protected String calculatePackagePath(URI packageUrl) {
        def path = new StringBuilder()
        if (packageUrl.scheme) path << packageUrl.scheme << '-'
        if (packageUrl.host) path << packageUrl.host << '-'
        if (packageUrl.port) path << packageUrl.port << '-'
        path << packageUrl.path.replace('/', '-') << '.zip'
        return path.toString()
    }

    protected String calculatePackagePath(String packageName, String packageVersion) {
        return packageName + (packageVersion ? '-' + packageVersion : '') + '.zip'
    }
}
