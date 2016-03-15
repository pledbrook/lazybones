package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 07/04/2014.
 */
interface PackageSourceManager {
    /**
     * Adds a package source to the search list. When asked for a package, the
     * manager will search all registered package sources to find it. Each
     * implementation of this interface can determine how it wants to deal with
     * attempts to register a package source with the same name as an existing
     * one, e.g. throw an exception, ignore it, return the already registered
     * one, etc.
     * @param repository The package source you want to add to the search list.
     * @return The registered package source. Normally this will be the same
     * as the object you passed in, but it may be different if a package source
     * with the same name has already been registered. It depends on the
     * implementation.
     */
    PackageSource registerPackageSource(PackageSource repository)

    /**
     * Returns the registered package source with the given name.
     * @param name The name of the package source you want.
     * @return The requested package source or @{code null} if there is no
     * package source with the given name.
     */
    PackageSource getPackageSource(String name)

    /**
     * Returns a list of all registered package sources. The order that they
     * are returned in is implementation dependent. If there are no registered
     * package sources, this method returns an empty list.
     */
    List<PackageSource> listPackageSources()
//    List<String> findPackages(String namePattern)

    /**
     * <p>Retrieves the details of a named package, if it is provided by any of
     * the registered package sources.</p>
     * Mp><b>Note</b> This interface does not define what happens when the
     * requested package is available in multiple package sources, nor does it
     * define the order in which the package sources are searched. It's up to
     * the implementation to define these behaviours.</p>
     * @param name The name of the package you want.
     * @return The requested package, or {@code null} if the package can't be
     * found in any of the registered package sources.
     */
    TemplateInfo getPackageInfo(String name)

    /**
     * Searches the package sources for the requested package and retrieves the
     * download URL for the requested version of that package.
     * @param name The name of the package you're interested in.
     * @param version The specific version of that package you want.
     * @return A URL from which you can download the requested template package,
     * or null if either the package or the specific version can't be found.
     */
    URI findTemplateUrl(String name, String version)
}
