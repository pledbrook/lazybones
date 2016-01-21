package uk.co.cacoethes.lazybones.impl

import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.api.LazybonesService
import uk.co.cacoethes.lazybones.api.PackageCache
import uk.co.cacoethes.lazybones.api.PackageInfo
import uk.co.cacoethes.lazybones.api.PackageSource
import uk.co.cacoethes.lazybones.api.PackageSourceManager
import uk.co.cacoethes.lazybones.api.TemplateInstaller

/**
 * Created by pledbrook on 13/04/2014.
 */
class DefaultLazybonesService implements LazybonesService {
    PackageSourceManager packageSourceManager
    PackageCache packageCache
    TemplateInstaller templateInstaller

    DefaultLazybonesService initFromDefaultConfig() {
        return this
    }

    @Override
    PackageSource registerPackageSource(PackageSource repository) {
        return null
    }

    @Override
    Map<PackageSource, List<String>> listTemplates() {
        return packageSourceManager.listPackageSources()*.listPackages()
    }

    @Override
    List<PackageInfo> findPackages(String namePattern) {
        return null
    }

    @Override
    PackageInfo getPackageInfo(String name) {
        return packageSourceManager
    }

    @Override
    String installTemplate(String name, String version, String targetPath, Map model = [:]) {
        def packageFile = packageCache.hasPackage(name, version) ?
                packageCache.getPackage(name, version) :
                fetchFromPackageSource(name, version)

        return installTemplate(packageFile, targetPath as File, model)
    }

    @Override
    String installTemplate(URI packageUrl, String targetPath, Map model = [:]) {
        def packageFile = packageCache.hasPackage(packageUrl) ?
                packageCache.getPackage(packageUrl) :
                packageCache.copyToCache(packageUrl)

        return installTemplate(packageFile, targetPath as File, model)
    }

    protected File fetchFromPackageSource(String name, String version) {
        def pkg = packageSourceManager.findTemplateUrl(name, version)
        if (pkg) {
            return packageCache.copyToCache(name, version, pkg)
        }
        else {
            throw new PackageNotFoundException(name, version)
        }
    }

    protected String installTemplate(File packageFile, File targetDir, Map model) {
        templateInstaller.createFromTemplate(packageFile, targetDir, model)
        return templateInstaller.getReadme(targetDir)
    }
}
