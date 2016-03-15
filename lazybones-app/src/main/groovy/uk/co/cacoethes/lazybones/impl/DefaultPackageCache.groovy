package uk.co.cacoethes.lazybones.impl

import groovy.util.logging.Log
import org.apache.commons.lang3.RandomStringUtils
import uk.co.cacoethes.lazybones.FileAlreadyExistsException
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.api.CachedPackage
import uk.co.cacoethes.lazybones.api.PackageCache
import uk.co.cacoethes.lazybones.api.PackageIdentifier

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
    File copyToCache(URI packageUrl, boolean overwrite) {
        final packageFile = findExistingPackageFile(packageUrl) ?: calculateCachePathForPackage(packageUrl)
        savePackageMetadata(copyToFile(packageFile, packageUrl, overwrite), [sourceUrl: packageUrl.toString()])
        return packageFile
    }

    @Override
    File copyToCache(PackageIdentifier pkgId, URI packageUrl, boolean overwrite) {
        final packageFile = copyToFile(calculateCachePathForPackage(pkgId), packageUrl, overwrite)
        savePackageMetadata(
                packageFile,
                [repoName: pkgId.repoName, name: pkgId.name, version: pkgId.version, sourceUrl: packageUrl.toString()])
        return packageFile
    }

    @Override
    File getPackage(URI packageUrl) {
        final file = findExistingPackageFile(packageUrl)
        if (!file) throw new PackageNotFoundException(packageUrl.toString())
        return file
    }

    @Override
    File getPackage(PackageIdentifier pkgId) {
        final file = calculateCachePathForPackage(pkgId)
        if (!file.exists()) throw new PackageNotFoundException(pkgId.name, pkgId.version)
        return file
    }

    @Override
    CachedPackage getPackageInfo(URI packageUrl) {
        final file = findExistingPackageFile(packageUrl)
        if (!file) throw new PackageNotFoundException(packageUrl.toString())
        return metadataToCachedPackage(loadPackageMetadata(file))
    }

    @Override
    CachedPackage getPackageInfo(PackageIdentifier pkgId) {
        final file = calculateCachePathForPackage(pkgId)
        if (!file.exists()) throw new PackageNotFoundException(pkgId.name, pkgId.version)
        return metadataToCachedPackage(loadPackageMetadata(file))
    }

    @Override
    CachedPackage findPackageInfo(String name) {
        return null
    }

    @Override
    CachedPackage findPackageInfo(String name, String version) {
        return null
    }

    @Override
    boolean hasPackage(URI packageUrl) {
        return findExistingPackageFile(packageUrl) != null
    }

    @Override
    boolean hasPackage(PackageIdentifier pkgId) {
        return calculateCachePathForPackage(pkgId).exists()
    }

    @Override
    List<CachedPackage> listPackages() {
        final cacheFiles = []
        cacheDir.eachFileRecurse { File f ->
            if (getMetadataFileForPackage(f).exists()) cacheFiles << f
        }

        return cacheFiles.collect { File f ->
            metadataToCachedPackage(loadPackageMetadata(f))
        }
    }

    /**
     * Copies the content at the given URI to a file location in the cache.
     * @param packageFile The path where the downloaded content will be written
     * to.
     * @param packageUrl The location of the content to download.
     * @param overwrite Determines whether this method should overwrite the
     * cache file if it already exists.
     * @return The path the cached file.
     * @throws FileAlreadyExistsException if the file is already in the cache
     * and {@code overwrite} is {@code false}.
     */
    protected File copyToFile(File packageFile, URI packageUrl, boolean overwrite) {
        if (packageFile.exists()) {
            if (!overwrite) throw new FileAlreadyExistsException(packageFile)
            else log.fine "Overwriting ${packageFile} in cache"
        }

        log.fine "Attempting to download ${packageUrl} into ${packageFile}"

        try {
            packageFile.parentFile.mkdirs()
            packageFile.withOutputStream { OutputStream out ->
                packageUrl.toURL().withInputStream { InputStream input ->
                    out << input
                }
            }
        }
        catch (Throwable t) {
            packageFile.deleteOnExit()
            throw t
        }
        return packageFile
    }

    protected File calculateCachePathForPackage(URI packageUrl) {
        return findUniquePackageFilename(
                new File(cacheDir, packageUrl.rawPath.substring(packageUrl.rawPath.lastIndexOf('/'))))
    }

    protected File calculateCachePathForPackage(PackageIdentifier pkgId) {
        return new File(cacheDir, pkgId.repoName + '/' + pkgId.name + (pkgId.version ? '-' + pkgId.version : '') + '.zip')
    }

    protected File[] findMatchingTemplates(Pattern pattern) {
        cacheDir.listFiles( { File f ->
            pattern.matcher(f.name).matches()
        } as FileFilter).sort { it.name }
    }

    protected File findExistingPackageFile(URI packageUri) {
        return cacheDir.listFiles({ File f ->
            getMetadataFileForPackage(f).exists()
        } as FileFilter).find { File f ->
            loadPackageMetadata(f)["sourceUrl"] == packageUri.toString()
        }
    }

    protected File findUniquePackageFilename(File baseFile) {
        final parentDir = baseFile.parentFile
        final baseName = baseFile.name - ".zip"
        def f = new File(parentDir, baseName + ".zip")
        while (f.exists()) {
            f = new File(parentDir, baseName + "-" + RandomStringUtils.randomAlphanumeric(6) + ".zip")
        }

        return f
    }

    protected CachedPackage metadataToCachedPackage(Map<String, String> metadata) {
        final pkgId = metadata["name"] ?
                new PackageIdentifier(metadata["repoName"], metadata["name"], metadata["version"]) :
                null
        return new CachedPackage(pkgId, new URI(metadata["sourceUrl"]))
    }

    protected Map<String, String> loadPackageMetadata(File packageFile) {
        final metadataFile = getMetadataFileForPackage(packageFile)
        if (!metadataFile.exists()) {
            throw new RuntimeException("Metadata file missing for cached package '${packageFile}'")
        }


        final props = new Properties()
        props.load(metadataFile.newReader("UTF-8"))
        return new HashMap<String, String>(props)
    }

    protected File savePackageMetadata(File packageFile, Map<String, String> data) {
        final props = new Properties()
        for (entry in data) props.setProperty(entry.key, entry.value)

        final metadataFile = getMetadataFileForPackage(packageFile)
        props.store(
                metadataFile.newWriter("UTF-8"),
                "Lazybones metadata for package file '${packageFile}'")
        return metadataFile
    }

    protected File getMetadataFileForPackage(File packageFile) {
        return new File(packageFile.parentFile, packageFile.name + ".properties")
    }
}
