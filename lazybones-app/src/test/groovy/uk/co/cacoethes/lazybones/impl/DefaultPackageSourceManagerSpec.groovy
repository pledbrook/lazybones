package uk.co.cacoethes.lazybones.impl

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import uk.co.cacoethes.lazybones.api.PackageInfo
import uk.co.cacoethes.lazybones.api.PackageSource

class DefaultPackageSourceManagerSpec extends Specification {
    @Shared psOne = new BintrayPackageSource("pledbrook/lazybones-templates")
    @Shared psTwo = new BintrayPackageSource("cacoethes")

    @Unroll
    def "Should return the named package source [#psName]"() {
        given: "A manager with a registered package sources"
        final psm = createPackageSourceManager()

        when: "I ask for a named package source"
        final source = psm.getPackageSource(psName)

        then: "The corresponding package source to be returned, or null if there is no source with that name"
        source == expected

        where:
        psName     | expected
        psOne.name | psOne
        psTwo.name | psTwo
        "unknown"  | null
    }

    def "Should return all registered package sources"() {
        given: "An initialised manager"
        final psm = createPackageSourceManager()
        final extra = new BintrayPackageSource("extra")
        psm.registerPackageSource(extra)

        expect: "All registered packages sources are returned in the order they're registered"
        psm.listPackageSources() == [psOne, psTwo, extra]
    }

    def "Should return an empty list when no package sources are registered"() {
        given: "An initialised manager with not registered package sources"
        final psm = new DefaultPackageSourceManager()

        expect: "An empty list"
        psm.listPackageSources() == []
    }

    def "Should return the details of a requested package if at least one package source hosts it"() {
        given: "Some package sources, one of which hosts the requested package"
        final expectedInfo = new PackageInfo(null, "ratpack", "1.2.1", null, null, null, null)
        final ps1 = Mock(PackageSource) {
            hasPackage(expectedInfo.name) >> false
            getPackage(expectedInfo.name) >> null
        }
        final ps2 = Mock(PackageSource) {
            hasPackage(expectedInfo.name) >> true
            getPackage(expectedInfo.name) >> expectedInfo
        }

        and: "A package source manager with those sources registered"
        final psm = new DefaultPackageSourceManager()
        psm.registerPackageSource(ps1)
        psm.registerPackageSource(ps2)

        expect: "I get the details of that package when requested"
        psm.getPackageInfo(expectedInfo.name) == expectedInfo
    }

    def "Should return the requested package info from the first source that hosts it"() {
        given: "Some package sources, all of which host the requested package"
        final expectedInfo = new PackageInfo(null, "ratpack", "1.2.1", null, null, null, null)
        final ps1 = Mock(PackageSource) {
            hasPackage(expectedInfo.name) >> true
            getPackage(expectedInfo.name) >> expectedInfo
        }
        final ps2 = Mock(PackageSource) {
            hasPackage(expectedInfo.name) >> true
            getPackage(expectedInfo.name) >> expectedInfo.copyWith(version: "1.0")
        }

        and: "A package source manager with those sources registered"
        final psm = new DefaultPackageSourceManager()
        psm.registerPackageSource(ps1)
        psm.registerPackageSource(ps2)

        expect: "I get the details of that package from the first package source"
        psm.getPackageInfo(expectedInfo.name) == expectedInfo
    }

    def "Should return null if requested package can't be found"() {
        given: "Some package sources, none of which hosts the requested package"
        final expectedInfo = new PackageInfo(null, "ratpack", "1.2.1", null, null, null, null)
        final ps1 = Mock(PackageSource) {
            hasPackage(expectedInfo.name) >> false
            getPackage(expectedInfo.name) >> null
        }
        final ps2 = Mock(PackageSource) {
            hasPackage(expectedInfo.name) >> false
            getPackage(expectedInfo.name) >> null
        }

        and: "A package source manager with those sources registered"
        final psm = new DefaultPackageSourceManager()
        psm.registerPackageSource(ps1)
        psm.registerPackageSource(ps2)

        expect: "I get null back for the requested package"
        psm.getPackageInfo(expectedInfo.name) == null
    }

    def "Should return null if no package sources are registered"() {
        given: "A package source manager with no registered package sources"
        final expectedInfo = new PackageInfo(null, "ratpack", "1.2.1", null, null, null, null)
        final psm = new DefaultPackageSourceManager()

        expect: "I get null back"
        psm.getPackageInfo(expectedInfo.name) == null
    }

    def "Should return a null template URL if no package sources are registered"() {
        given: "A package source manager with no registered package sources"
        final expectedInfo = new PackageInfo(null, "ratpack", "1.2.1", null, null, null, null)
        final psm = new DefaultPackageSourceManager()

        expect: "I get null back"
        psm.findTemplateUrl(expectedInfo.name, "1.1") == null
    }

    def "Should return the URL for the requested package version"() {
        given: "A package source manager with a single registered package source"
        final pkgName = "ratpack"
        final pkgVersion = "1.2.1"
        final ps1 = Mock(PackageSource) {
            hasPackage(pkgName) >> false
            getPackage(pkgName) >> null
        }
        final expectedInfo = new PackageInfo(null, "ratpack", "1.2.1", null, null, null, null)
        final psm = new DefaultPackageSourceManager()

        expect: "I get null back"
        psm.findTemplateUrl(expectedInfo.name, "1.1") == null
    }

    private DefaultPackageSourceManager createPackageSourceManager() {
        final psm = new DefaultPackageSourceManager()
        psm.registerPackageSource(psOne)
        psm.registerPackageSource(psTwo)
        return psm
    }

}
