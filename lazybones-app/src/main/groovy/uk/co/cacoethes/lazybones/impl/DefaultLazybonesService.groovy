package uk.co.cacoethes.lazybones.impl

import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.api.CachedPackage
import uk.co.cacoethes.lazybones.api.LazybonesService
import uk.co.cacoethes.lazybones.api.PackageCache
import uk.co.cacoethes.lazybones.api.TemplateInfo
import uk.co.cacoethes.lazybones.api.PackageSource
import uk.co.cacoethes.lazybones.api.PackageSourceManager
import uk.co.cacoethes.lazybones.api.NewProjectInfo
import uk.co.cacoethes.lazybones.api.TemplateInstaller
import uk.co.cacoethes.lazybones.config.Configuration

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by pledbrook on 13/04/2014.
 */
@Singleton
class DefaultLazybonesService implements LazybonesService {
    PackageSourceManager packageSourceManager
    PackageCache packageCache
    TemplateInstaller templateInstaller

    @Inject
    DefaultLazybonesService(
            PackageSourceManager psm,
            PackageCache pc,
            TemplateInstaller ti) {
        packageSourceManager = psm
        packageCache = pc
        templateInstaller = ti
    }

    DefaultLazybonesService initFromConfig(Configuration config) {
        for (String repo in config.getSetting("bintrayRepositories")) {
            registerPackageSource(new BintrayPackageSource(repo))
        }
        return this
    }

    @Override
    PackageSource registerPackageSource(PackageSource repository) {
        return packageSourceManager.registerPackageSource(repository)
    }

    @Override
    Map<PackageSource, List<String>> listTemplates() {
        return packageSourceManager.listPackageSources()*.listPackages()
    }

    @Override
    List<CachedPackage> listCachedTemplates() {
        return packageCache.listPackages()
    }

    @Override
    List<TemplateInfo> findPackages(String namePattern) {
        return null
    }

    @Override
    TemplateInfo getPackageInfo(String name) {
        return packageSourceManager
    }

    NewProjectInfo installTemplate(
            String name,
            String version,
            String targetPath,
            List<String> tmplQualifiers,
            Map model = [:]) {
        def packageFile = packageCache.hasPackage(name, version) ?
                packageCache.getPackage(name, version) :
                fetchFromPackageSource(name, version)

        return installTemplate(packageFile, targetPath as File, tmplQualifiers, model)
    }

    NewProjectInfo installTemplate(
            URI packageUrl,
            String targetPath,
            List<String> tmplQualifiers,
            Map model = [:]) {
        def packageFile = packageCache.hasPackage(packageUrl) ?
                packageCache.getPackage(packageUrl) :
                packageCache.copyToCache(packageUrl)

        return installTemplate(packageFile, targetPath as File, tmplQualifiers, model)
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

    protected NewProjectInfo installTemplate(File packageFile, File targetDir, List<String> tmplQualifiers, Map model) {
        return templateInstaller.installTemplate(packageFile, targetDir, tmplQualifiers, model)
    }

    @Override
    boolean isLazybonesProjectDir(File dir) {
        return false
    }

    @Override
    List<String> listSubtemplates() {
        return null
    }
}
