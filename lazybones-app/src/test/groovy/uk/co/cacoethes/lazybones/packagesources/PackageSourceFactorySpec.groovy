package uk.co.cacoethes.lazybones.packagesources

import spock.lang.Specification
import uk.co.cacoethes.lazybones.config.Configuration

class PackageSourceFactorySpec extends Specification {
    PackageSourceBuilder packageSourceFactory

    final static expectedBintrayRepositories = ['repo1', 'repo2', 'repo3']

    void 'returns a list of Bintray package sources from the ConfigObject'() {
        given: 'A package source factory'
        packageSourceFactory = new PackageSourceBuilder()

        when: 'the package name doesn\'t start with http://'
        List<PackageSource> packageSources = packageSourceFactory.buildPackageSourceList(
                initConfig(bintrayRepositories: expectedBintrayRepositories))

        then: 'there is a BintrayPackageSource for each of the expectedBintrayRepositories'
        packageSources.collect { it.repoName } == expectedBintrayRepositories
    }

    protected Configuration initConfig(Map settings) {
        return new Configuration(
                new ConfigObject(),
                settings,
                [:],
                ["bintrayRepositories": String[]],
                new File("delete-me.json"))
    }
}
