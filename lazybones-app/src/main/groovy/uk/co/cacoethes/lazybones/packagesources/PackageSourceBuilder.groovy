package uk.co.cacoethes.lazybones.packagesources

import uk.co.cacoethes.lazybones.config.Configuration

/**
 * Factory class for generating package sources, i.e. repositories that provide
 * Lazybones template packages.
 */
class PackageSourceBuilder {
    /**
     * Builds an ordered list of package sources which could provide the given package name.
     *
     * @param packageName
     * @param config
     * @return
     */
    List<PackageSource> buildPackageSourceList(Configuration config) {
        List<String> repositoryList = (List) config.getSetting("bintrayRepositories")
        return repositoryList.collect { new BintrayPackageSource(it) }
    }
}
