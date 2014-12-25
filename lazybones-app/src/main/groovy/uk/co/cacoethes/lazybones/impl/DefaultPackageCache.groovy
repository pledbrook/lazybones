package uk.co.cacoethes.lazybones.impl

import groovy.util.logging.Log
import org.apache.commons.codec.digest.DigestUtils
import uk.co.cacoethes.lazybones.api.CachedPackage
import uk.co.cacoethes.lazybones.api.PackageCache

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import java.util.regex.Pattern

/**
 * Created by pledbrook on 11/04/2014.
 */
@Log
@Singleton
class DefaultPackageCache implements PackageCache {
    static final String VERSION_PATTERN = /\d+\.\d[^-]*(?:-SNAPSHOT)?/
    static final Pattern TEMPLATE_URL_PATTERN = ~/^(?:file|http|https)-[0-9a-z]{32}-(.*)\.zip$/
    static final Pattern TEMPLATE_NAME_PATTERN = ~/^(.*)-($VERSION_PATTERN)\.zip$/

    private File cacheDir

    @Inject
    DefaultPackageCache(@Named("cache.dir") File cacheDir) {
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
        return new File(cacheDir, calculateCachedFilename(packageUrl))
    }

    @Override
    File getPackage(String name, String version) {
        return new File(cacheDir, calculateCachedFilename(name, version))
    }

    @Override
    CachedPackage getPackageInfo(URI packageUrl) {
        return null
    }

    @Override
    CachedPackage getPackageInfo(String name, String version) {
        return null
    }

    @Override
    boolean hasPackage(URI packageUrl) {
        return getPackage(packageUrl).exists()
    }

    @Override
    boolean hasPackage(String name, String version) {
        return getPackage(name, version).exists()
    }

    @Override
    List<CachedPackage> listPackages() {

        return cacheDir.listFiles { String name ->
            new File(name + ".src").exists()
        }.groupBy { File f ->
            def m = TEMPLATE_NAME_PATTERN.matcher(f.name).group(1)
        }.collectEntries { String tmplName, List<File> files ->
            // Extract the version numbers and make those the key value.
            [ tmplName, files.collect { TEMPLATE_NAME_PATTERN.matcher(it.name)[0][2] } ]
        }
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
        finally {
            packageFile.deleteOnExit()
        }
        return packageFile
    }

    protected String calculateCachedFilename(URI packageUrl) {
        final path = packageUrl.path
        final filename = new StringBuilder()
        if (packageUrl.scheme) filename << packageUrl.scheme << '-'
        filename << DigestUtils.md5Hex(packageUrl.toString()) << '-'
        filename << path.substring(path.lastIndexOf('/') + 1)
        if (!path.endsWith(".zip")) filename << ".zip"
        return filename.toString()
    }

    protected String calculateCachedFilename(String packageName, String packageVersion) {
        return packageName + (packageVersion ? '-' + packageVersion : '') + '.zip'
    }

    protected File[] findMatchingTemplates(Pattern pattern) {
        cacheDir.listFiles( { File f ->
            pattern.matcher(f.name).matches()
        } as FileFilter).sort { it.name }
    }
}
