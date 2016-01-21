package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 07/04/2014.
 */
interface PackageSourceManager {
    PackageSource registerPackageSource(PackageSource repository)
    PackageSource getPackageSource(String name)
    List<PackageSource> listPackageSources()
    List<String> findPackages(String namePattern)
    PackageInfo getPackageInfo(String name)
    URI findTemplateUrl(String name, String version)
}
