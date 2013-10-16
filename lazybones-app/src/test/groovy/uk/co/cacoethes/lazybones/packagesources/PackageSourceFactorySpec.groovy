package uk.co.cacoethes.lazybones.packagesources

import spock.lang.Specification

class PackageSourceFactorySpec extends Specification {
    PackageSourceBuilder packageSourceFactory

    final static expectedBintrayRepositories = ['repo1', 'repo2', 'repo3']

    void 'returns a list of Bintray package sources from the ConfigObject'() {
        given: 'A package source factory'
        packageSourceFactory = new PackageSourceBuilder()

        when: 'the package name doesn\'t start with http://'
        List<PackageSource> packageSources = packageSourceFactory.buildPackageSourceList(
                new ConfigObject(bintrayRepositories: expectedBintrayRepositories))

        then: 'there is a BintrayPackageSource for each of the expectedBintrayRepositories'
        packageSources.collect { it.repoName } == expectedBintrayRepositories
    }
}
