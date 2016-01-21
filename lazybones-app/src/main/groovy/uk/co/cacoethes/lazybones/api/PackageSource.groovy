package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 11/04/2014.
 */
interface PackageSource {
    /**
     * Returns the assigned name of this package source. This is implementation
     * dependent, but has no real effect on Lazybones. It's just a label/reference.
     */
    String getName()

    /**
     * Returns the number of packages available from this packages source.
     */
    int getPackageCount()

    /**
     * Returns a list of the available packages. If there are no packages, this
     * returns an empty list.
     * @param options A map of named arguments for managing batching of the
     * results. An option of "offset" specifies the index position of the first
     * package to return (indexed from 0). "max" specifies the maximum number of
     * packages to return.
     */
    List<PackageInfo> listPackages(Map options)

    /**
     * Returns whether this package source contains the given package.
     */
    boolean hasPackage(String name)

    /**
     * Returns details about a given package. If no package is found with the
     * given name, this returns {@code null}.
     */
    PackageInfo getPackage(String name)

    /**
     * Returns the URL for a specific version of a package.
     * @param name The name of the package you want.
     * @param version The specific version of the package you want.
     * @throws {@link uk.co.cacoethes.lazybones.PackageNotFoundException} if
     * either the package or that particular version don't exist.
     */
    URI getTemplateUrl(String name, String version)
}
