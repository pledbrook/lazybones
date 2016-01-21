package uk.co.cacoethes.lazybones.impl

import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.api.PackageInfo
import uk.co.cacoethes.lazybones.api.PackageSource
import uk.co.cacoethes.lazybones.api.PackageSourceManager

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

    @Override
    PackageSource registerBintrayPackageSource(String repositoryName) {
        def pkgSrc = new BintrayPackageSource(repositoryName)
        packageSourceList << pkgSrc
        return pkgSrc
    }

    @Override
    List<PackageSource> listPackageSources() {
        return new ArrayList(packageSourceList)
    }

    @Override
    PackageSource getPackageSource(String name) {
        return packageSourceList.find { it.name == name }
    }

    @Override
    PackageInfo findPackage(String name) {
        return packageSourceList.find { PackageSource src -> src.hasPackage(name) }?.getPackage(name)
    }

    @Override
    URI findTemplateUrl(String name, String version) {
        def pkgSrc = packageSourceList.find { PackageSource src -> src.hasPackage(name) }
        if (pkgSrc && pkgSrc.getPackage(name).hasVersion(version)) {
            return pkgSrc.getTemplateUrl(name, version)
        }
        return null
    }
}
