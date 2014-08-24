package uk.co.cacoethes.lazybones.packagesources

/**
 * Factory class for generating package sources, i.e. repositories that provide
 * Lazybones template packages.
 */
class PackageSourceBuilder {
    /**
     * Builds an ordered list of package sources which could provide the given package name.
     *
     * @param packageName
     * @param configObject
     * @return
     */
    List<PackageSource> buildPackageSourceList(ConfigObject configObject) {
        List<String> repositoryList = (List) configObject.bintrayRepositories
        return repositoryList.collect { new BintrayPackageSource(it) }
    }
}
