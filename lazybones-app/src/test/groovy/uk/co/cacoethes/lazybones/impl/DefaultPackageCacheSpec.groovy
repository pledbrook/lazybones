package uk.co.cacoethes.lazybones.impl

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll
import uk.co.cacoethes.lazybones.FileAlreadyExistsException
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.lazybones.api.CachedPackage
import uk.co.cacoethes.lazybones.api.PackageIdentifier

/**
 * WARNING: many of these tests depend on {@code copyToCache()} working correctly,
 * so if it's not, test failures will cascade. I've taken this approach in order
 * to make the unit tests as black-box as possible.
 */
class DefaultPackageCacheSpec extends Specification {
    static final String DEFAULT_REPO_NAME = "pledbrook/lazybones"
    
    @Shared String testPackageFilename = "ratpack-custom-1.2.1.zip"
    @Shared String testPackagePath = "/" + testPackageFilename
    @Shared PackageIdentifier testPackageId = new PackageIdentifier(DEFAULT_REPO_NAME, "ratpack", "1.2.2")
    @Shared URI testPackageUri = getClass().getResource(testPackagePath).toURI()

    @Rule Recorder recorder = new Recorder()
    @Rule TemporaryFolder testDir = new TemporaryFolder()

    @Betamax(tape="packages")
    def "Should copy an existing URL-only package to the file cache"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = getClass().getResource(testPackagePath).toURI()

        when: "I copy the package at the given URL to the cache"
        final cachedFile = dpc.copyToCache(testPkg, false)

        then: "The cache should contain a file with the same contents and a name based on the URL"
//        cachedFile.name == createCachedFilename(testPkg, testPackageFilename)
        cachedFile.parentFile == cacheDir
        cachedFile.bytes == testPkg.toURL().bytes
    }

    def "Should throw an exception if the package is already in the cache when copying"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = getClass().getResource(testPackagePath).toURI()

        and: "The target file already in the cache"
        dpc.copyToCache(testPkg, false)

        when: "I copy a package that has already been put in the cache"
        dpc.copyToCache(testPkg, false)

        then: "An exception is thrown"
        FileAlreadyExistsException ex = thrown()
        ex.message.contains(testPackageFilename)
    }

    def "Should overwrite files in cache when requested by a copy"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = getClass().getResource(testPackagePath).toURI()

        and: "The target file already in the cache"
        def f = dpc.copyToCache(testPkg, false)
        def expectedSize = f.size()
        f.setText("Hello world", "UTF-8")
        assert f.size() != expectedSize

        when: "I copy a package that has already been put in the cache"
        f = dpc.copyToCache(testPkg, true)

        then: "The file is overwritten"
        f.size() == expectedSize
    }

    def "Should copy the requested version of a named package to the cache"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = getClass().getResource(testPackagePath).toURI()

        when: "I copy a named and versioned package from a given URL to the cache"
        final cachedFile = dpc.copyToCache(testPackageId, testPkg, false)

        then: "The cache should contain a file with the same contents and a name based on the package name/version"
        cachedFile.name == "ratpack-1.2.2.zip"
        cachedFile.parentFile.canonicalPath.startsWith(cacheDir.canonicalPath)
        cachedFile.bytes == testPkg.toURL().bytes
    }

    def "Should throw an exception when copying a named & versioned package that's already in the cache"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = getClass().getResource(testPackagePath).toURI()

        and: "The target package already in the cache"
        dpc.copyToCache(testPackageId, testPkg, false)

        when: "I copy a named and versioned package from a given URL to the cache"
        final cachedFile = dpc.copyToCache(testPackageId, testPkg, false)

        then: "The method should throw an exception"
        FileAlreadyExistsException ex = thrown()
        ex.message.contains "ratpack"
        ex.message.contains "1.2.2"
    }

    def "Should overwrite files in cache when requested by a copy using package name & version"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = getClass().getResource(testPackagePath).toURI()

        and: "The target file already in the cache"
        def f = dpc.copyToCache(testPackageId, testPkg, false)
        def expectedSize = f.size()
        f.setText("Hello world", "UTF-8")
        assert f.size() != expectedSize

        when: "I copy a package that has already been put in the cache"
        f = dpc.copyToCache(testPackageId, testPkg, true)

        then: "The file is overwritten"
        f.size() == expectedSize
    }

    def "Should throw an exception if the package URL returns a 404"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = new File(testDir.newFolder(), testPackagePath).toURI()

        when: "I copy a package from a URL that doesn't exist"
        dpc.copyToCache(testPkg, false)

        then: "A FileNotFoundException is thrown"
        FileNotFoundException ex = thrown()
        ex.message.contains(testPackagePath)
    }

    def "Should throw an exception if the package URL returns a 404 (w/ package name & version)"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = new File(testDir.newFolder(), testPackagePath).toURI()

        when: "I copy a package from a URL that doesn't exist"
        dpc.copyToCache(testPackageId, testPkg, false)

        then: "A FileNotFoundException is thrown"
        FileNotFoundException ex = thrown()
        ex.message.contains(testPackagePath)
    }

    def "Should retrieve requested package file from the cache based on a package URL"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())
        final cachedFile = dpc.copyToCache(testPkgUri, false)

        expect: "The path to the cached file associated with the given package URL"
        dpc.getPackage(testPkgUri) == cachedFile
    }

    def "Should retrieve requested package file from the cache based on a package name and version"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()
        final pkgName = testPackageId.name
        final pkgVersion = testPackageId.version

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())
        final cachedFile = dpc.copyToCache(testPackageId, testPkgUri, false)

        expect: "The path to the cached file associated with the given package coordinates"
        dpc.getPackage(testPackageId) == cachedFile
    }

    def "Should throw an exception if the requested package file (based on a URL origin) isn't in the cache"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())

        when: "I request the package from the cache"
        dpc.getPackage(testPkgUri)

        then: "An appropriate exception is thrown"
        PackageNotFoundException ex = thrown()
        ex.message.contains(testPkgUri.toString())
    }

    def "Should throw an exception if cached package comes from a different repo to the requested one"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()
        final pkgName = "ratpack"
        final pkgVersion = "1.2.2"

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())
        dpc.copyToCache(testPackageId, testPkgUri, false)

        when: "I request the template package from the cache"
        dpc.getPackage(testPackageId.copyWith(repoName: "other/repo"))

        then: "An appropriate exception is thrown"
        PackageNotFoundException ex = thrown()
        ex.message.contains(pkgName)
        ex.message.contains(pkgVersion)
    }

    def "Should throw an exception if the requested package (based on name and version) isn't in the cache"() {
        given: "A test package URI"
        final pkgName = "ratpack"
        final pkgVersion = "1.2.2"

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())

        when: "I request the template package from the cache"
        dpc.getPackage(testPackageId)

        then: "An appropriate exception is thrown"
        PackageNotFoundException ex = thrown()
        ex.message.contains(pkgName)
        ex.message.contains(pkgVersion)
    }

    def "Should return info about a URL-based package"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())
        dpc.copyToCache(testPkgUri, false)

        expect: "The details of the cached package"
        dpc.getPackageInfo(testPkgUri) == new CachedPackage(null, testPkgUri)
    }

    def "Should return info about a named package in the cache"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())
        dpc.copyToCache(testPackageId, testPkgUri, false)

        expect: "The details of the cached package"
        dpc.getPackageInfo(testPackageId) == new CachedPackage(testPackageId, testPkgUri)
    }

    def "Should throw exception when requesting info for a URL-based package that's not in the cache"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache"
        final dpc = new DefaultPackageCache(testDir.newFolder())

        when: "I request the details of the un-cached package"
        dpc.getPackageInfo(testPkgUri)

        then: "An exception is thrown"
        PackageNotFoundException ex = thrown()
        ex.message.contains(testPkgUri.toString())
    }

    def "Should throw exception when requesting info for a named package that's not in the cache"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache"
        final dpc = new DefaultPackageCache(testDir.newFolder())

        when: "I request the details of the un-cached package"
        dpc.getPackageInfo(testPackageId)

        then: "An exception is thrown"
        PackageNotFoundException ex = thrown()
        ex.message.contains(testPackageId.name)
        ex.message.contains(testPackageId.version)
    }

    def "Should return the info of the requested package when there is only one matching"() {

    }

    def "Should return null if requested package can't be found in the cache"() {

    }

    @Unroll
    def "Should determine that a cached file #exists for a given package URL"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())
        if (expected) {
            dpc.copyToCache(testPkgUri, false)
        }

        expect: "true if the package is cached, otherwise false"
        dpc.hasPackage(testPkgUri) == expected

        where:
        expected << [true, false]
        exists = expected ? "exists" : "does not exist"

    }

    @Unroll
    def "Should determine if a cached file exists for a given package name and version"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())
        if (doCopy) {
            dpc.copyToCache(testPackageId, testPkgUri, false)
        }

        expect: "The path to the cached file associated with the given package coordinates"
        dpc.hasPackage(pkgId) == expected

        where:
        pkgId                                          | expected | doCopy
        testPackageId                                  | true     | true
        testPackageId                                  | false    | false
        testPackageId.copyWith(repoName: "other/repo") | false    | true
        testPackageId.copyWith(version: "1.0.1")       | false    | true
    }

    @Unroll
    def "Should return a list of cached packages"() {
        given: "A test package URI"
        final testPkgUri = getClass().getResource(testPackagePath).toURI()

        and: "An initialised package cache with a cached file"
        final dpc = new DefaultPackageCache(testDir.newFolder())
        for (pkg in expected) {
            if (pkg.id) {
                dpc.copyToCache(pkg.id, pkg.sourceUri, false)
            }
            else {
                dpc.copyToCache(pkg.sourceUri, false)
            }
        }

        expect: "The path to the cached file associated with the given package coordinates"
        dpc.listPackages() as Set == expected as Set

        where:
        expected << [
                [],
                [new CachedPackage(null, testPackageUri)],
                [new CachedPackage(testPackageId, testPackageUri)],
                [
                        new CachedPackage(null, testPackageUri),
                        new CachedPackage(testPackageId, testPackageUri),
                        new CachedPackage(testPackageId.copyWith(version: "1.1.0"), testPackageUri),
                        new CachedPackage(testPackageId.copyWith(repoName: "other/repo"), testPackageUri)
                ]
        ]
    }
}
