package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.PackageInfo
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.packagesources.PackageSource

/**
 * Builds a PackageLocation object based on the command info from the
 */
@Log
class PackageLocationBuilder {
    static final File INSTALL_DIR = new File(System.getProperty('user.home'), ".lazybones/templates")

    PackageLocation buildPackageLocation(CreateCommandInfo commandInfo, List<PackageSource> packageSources) {
        if (packageNameIsAUrl(commandInfo.packageName)) {
            return buildForUrl(commandInfo)
        }

        buildForBintray(commandInfo, packageSources)
    }

    private boolean packageNameIsAUrl(String packageName) {
        packageName.startsWith("http://") || packageName.startsWith("https://")
    }

    private PackageLocation buildForUrl(CreateCommandInfo commandInfo) {
        String cacheLocation = cacheLocationPattern(commandInfo.packageName, null)
        return new PackageLocation(remoteLocation: commandInfo.packageName, cacheLocation: cacheLocation)
    }

    private PackageLocation buildForBintray(CreateCommandInfo commandInfo, List<PackageSource> packageSources) {
        if (commandInfo.requestedVersion) {
            String cacheLocation = cacheLocationPattern(commandInfo.packageName, commandInfo.requestedVersion)
            File cacheFile = new File(cacheLocation)
            if (cacheFile.exists()) {
                return new PackageLocation(cacheLocation: cacheLocation)
            }
        }

        PackageInfo packageInfo = getPackageInfo(commandInfo.packageName, packageSources)
        String versionToDownload = commandInfo.requestedVersion ?: packageInfo.latestVersion
        String cacheLocation = cacheLocationPattern(commandInfo.packageName, versionToDownload)
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
        INSTALL_DIR.absolutePath + '/' + name + (version ? '-' + version : '') + '.zip'

    }
}
