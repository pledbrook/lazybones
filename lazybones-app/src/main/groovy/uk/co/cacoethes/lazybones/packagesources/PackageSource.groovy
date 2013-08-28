package uk.co.cacoethes.lazybones.packagesources

import uk.co.cacoethes.lazybones.PackageInfo

/**
 * Represents a source of information about Lazybones packaged templates. This
 * could be a REST service, a cached file, or something else.
 */
interface PackageSource {
    /**
     * Returns a list of the available packages. If there are no packages, this
     * returns an empty list.
     */
    List<String> listPackageNames()

    /**
     * Returns details about a given package. If no package is found with the
     * given name, this returns {@code null}.
     */
    PackageInfo fetchPackageInfo(String packageName)

    /**
     * Returns the URL to download particular package and version from this package source
     */
    String getTemplateUrl(String pkgName, String version)
}
