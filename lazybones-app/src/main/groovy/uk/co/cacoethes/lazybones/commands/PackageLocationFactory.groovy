package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.PackageInfo
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.packagesources.PackageSource

/**
 * Builds a PackageLocation object based on the command info from the
 */
@Log
class PackageLocationFactory {
    static final File INSTALL_DIR = new File(System.getProperty('user.home'), ".lazybones/templates")

    PackageLocation createPackageLocation(CreateCommandInfo commandInfo, List<PackageSource> packageSources) {
        if (packageNameIsAUrl(commandInfo.packageName)) {
            return createForUrl(commandInfo)
        }

        createForBintray(commandInfo, packageSources)
    }

    private boolean packageNameIsAUrl(String packageName) {
        packageName.startsWith("http://") || packageName.startsWith("https://")
    }

    private PackageLocation createForUrl(CreateCommandInfo commandInfo) {
        String cacheLocation = INSTALL_DIR.absolutePath + '/' + commandInfo.packageName.split('/').last() + '.zip'
        return new PackageLocation(remoteLocation: commandInfo.packageName, cacheLocation: cacheLocation)
    }

    private PackageLocation createForBintray(CreateCommandInfo commandInfo, List<PackageSource> packageSources) {
        try {
            PackageInfo packageInfo = getPackageInfo(commandInfo.packageName, packageSources)
            String versionToDownload = commandInfo.requestedVersion ?: packageInfo.latestVersion
            String cacheLocation = INSTALL_DIR.absolutePath + '/' + commandInfo.packageName + '-' + versionToDownload + '.zip'
            String remoteLocation = packageInfo.source.getTemplateUrl(packageInfo.name, versionToDownload)

            return new PackageLocation(remoteLocation: remoteLocation, cacheLocation: cacheLocation)
        } catch (PackageNotFoundException e) {
            if (commandInfo.requestedVersion) {
                String cacheLocation = INSTALL_DIR.absolutePath + '/' + commandInfo.packageName + '-' + commandInfo.requestedVersion + '.zip'
                File cacheFile = new File(cacheLocation)
                if (cacheFile.exists()) {
                    return new PackageLocation(cacheLocation: cacheLocation)
                }
            }

            throw e
        }

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
}
