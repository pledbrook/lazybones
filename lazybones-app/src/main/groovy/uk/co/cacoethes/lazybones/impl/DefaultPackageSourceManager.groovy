package uk.co.cacoethes.lazybones.impl

import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.api.PackageInfo
import uk.co.cacoethes.lazybones.api.PackageSource
import uk.co.cacoethes.lazybones.api.PackageSourceManager

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by pledbrook on 11/04/2014.
 */
class DefaultPackageSourceManager implements PackageSourceManager {
    List<PackageSource> packageSourceList = []

    @Override
    PackageSource registerPackageSource(PackageSource packageSource) {
        packageSourceList << packageSource
        return packageSource
    }

    /**
     * Returns a list of the registered package sources in the order that they
     * were registered. Modifying the returned list has no impact on the manager
     * itself as the method returns a copy of the internal list.
     */
    @Override
    List<PackageSource> listPackageSources() {
        return new ArrayList(packageSourceList)
    }

//    @Override
    List<PackageInfo> findPackages(String namePattern) {
        return null
    }

    /**
     * Fetches the details for a given package if any of the registered package
     * sources hosts it. If more than one package source has a package with the
     * given name, then the first one that was registered wins. If no package
     * sources have been registered, the package will be treated as not found
     * and you'll get {@code null} as the return value.
     * @param name The name of the package you're interested in.
     * @return The details of the requested package, or {@code null} if the
     * package can't be found.
     */
    @Override
    PackageInfo getPackageInfo(String name) {
        return packageSourceList.find { PackageSource src -> src.hasPackage(name) }?.getPackage(name)
    }

    @Override
    PackageSource getPackageSource(String name) {
        return packageSourceList.find { it.name == name }
    }

    @Override
    URI findTemplateUrl(String name, String version) {
        return packageSourceList.find { PackageSource src ->
            src.hasPackage(name) && src.getPackage(name).hasVersion(version)
        }?.getTemplateUrl(name, version)
    }
}
