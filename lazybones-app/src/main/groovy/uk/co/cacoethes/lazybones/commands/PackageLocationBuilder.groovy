package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import org.apache.commons.io.FilenameUtils
import uk.co.cacoethes.lazybones.PackageInfo
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.packagesources.PackageSource

/**
 * Builds a PackageLocation object based on the command info from the
 */
@Log
class PackageLocationBuilder {
    static final String DEFAULT_CACHE_PATH =
            FilenameUtils.concat(System.getProperty('user.home'), ".lazybones/templates")

    final File cacheDir = new File(System.getProperty("lazybones.cacheDir") ?: DEFAULT_CACHE_PATH)

    PackageLocationBuilder() { }

    PackageLocationBuilder(File cacheDir) {
        this.cacheDir = cacheDir
    }

    PackageLocation buildPackageLocation(String packageName, String version, List<PackageSource> packageSources) {
        if (isUrl(packageName)) {
            return buildForUrl(packageName)
        }

        buildForBintray(packageName, version, packageSources)
    }

    /**
     * Determines whether the given package name is in fact a full blown URI,
     * including scheme.
     */
    private boolean isUrl(String str) {
        try {
            def uri = new URI(str)
            return uri.scheme
        }
        catch (URISyntaxException ex) {
            return false
        }
    }

    private PackageLocation buildForUrl(String url) {
        def packageName = FilenameUtils.getBaseName(new URI(url).path)

        return new PackageLocation(remoteLocation: url, cacheLocation: cacheLocationPattern(packageName, null))
    }

    private PackageLocation buildForBintray(String packageName, String version, List<PackageSource> packageSources) {
        if (version) {
            String cacheLocation = cacheLocationPattern(packageName, version)
            File cacheFile = new File(cacheLocation)
            if (cacheFile.exists()) {
                return new PackageLocation(cacheLocation: cacheLocation)
            }
        }

        PackageInfo packageInfo = getPackageInfo(packageName, packageSources)
        String versionToDownload = version ?: packageInfo.latestVersion
        String cacheLocation = cacheLocationPattern(packageName, versionToDownload)
        String remoteLocation = packageInfo.source.getTemplateUrl(packageInfo.name, versionToDownload)

        return new PackageLocation(remoteLocation: remoteLocation, cacheLocation: cacheLocation)
    }

    protected PackageInfo getPackageInfo(String packageName, List<PackageSource> packageSources) {
        for (PackageSource packageSource in packageSources) {
            log.fine "Searching for ${packageName} in ${packageSource}"

            def pkgInfo = packageSource.fetchPackageInfo(packageName)
            if (pkgInfo) {
                log.fine "Found!"
                return pkgInfo
            }
        }

        throw new PackageNotFoundException(packageName)
    }

    private String cacheLocationPattern(String name, String version) {
        cacheDir.absolutePath + '/' + name + (version ? '-' + version : '') + '.zip'
    }
}
