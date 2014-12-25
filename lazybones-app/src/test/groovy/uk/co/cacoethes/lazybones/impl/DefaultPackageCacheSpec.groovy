package uk.co.cacoethes.lazybones.impl

import org.apache.commons.codec.digest.DigestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import uk.co.cacoethes.lazybones.api.CachedPackage

class DefaultPackageCacheSpec extends Specification {
    @Shared String testPackageFilename = "ratpack-custom-1.2.1.zip"

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder()

    def "Should copy an existing URL-only package to the file cache"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = getClass().getResource(testPackageFilename).toURI()

        when: "I copy the package at the given URL to the cache"
        final cachedFile = dpc.copyToCache(testPkg)

        then: "The cache should contain a file with the same contents and a name based on the URL"
        cachedFile.name == createCachedFilename(testPkg, testPackageFilename)
        cachedFile.parentFile == cacheDir
        cachedFile.bytes == testPkg.toURL().bytes
    }

    def "Should throw an exception if the package URL returns a 404"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = new File(testDir.newFolder(), testPackageFilename).toURI()

        when: "I copy a package from a URL that doesn't exist"
        dpc.copyToCache(testPkg)

        then: "A FileNotFoundException is thrown"
        FileNotFoundException ex = thrown()
        ex.message.contains(testPackageFilename)
    }

    def "Should copy the requested version of a named package to the cache"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = getClass().getResource(testPackageFilename).toURI()

        when: "I copy a named and versioned package from a given URL to the cache"
        final cachedFile = dpc.copyToCache("ratpack", "1.2.2", testPkg)

        then: "The cache should contain a file with the same contents and a name based on the package name/version"
        cachedFile.name == "ratpack-1.2.2.zip"
        cachedFile.parentFile == cacheDir
        cachedFile.bytes == testPkg.toURL().bytes
    }

    def "Should throw an exception if the package URL returns a 404 (w/ package name & version)"() {
        given: "An initialised package cache"
        final cacheDir = testDir.newFolder()
        final dpc = new DefaultPackageCache(cacheDir)

        and: "A test package URI"
        final testPkg = new File(testDir.newFolder(), testPackageFilename).toURI()

        when: "I copy a package from a URL that doesn't exist"
        dpc.copyToCache("ratpack", "1.2.2", testPkg)

        then: "A FileNotFoundException is thrown"
        FileNotFoundException ex = thrown()
        ex.message.contains(testPackageFilename)
    }

    def "Should retrieve requested package file from the cache based on a package URL"() {
        given: "A test package URI"
        final testPkg = getClass().getResource(testPackageFilename).toURI()

        and: "An initialised package cache with a cached file"
        final cachedFile = createCachedFile(testPkg, testPackageFilename)
        final dpc = new DefaultPackageCache(cachedFile.parentFile)

        expect: "The path to the cached file associated with the given package URL"
        dpc.getPackage(testPkg) == cachedFile
    }

    def "Should retrieve requested package file from the cache based on a package name and version"() {
        given: "A test package URI"
        final testPkg = getClass().getResource(testPackageFilename).toURI()
        final pkgName = "ratpack"
        final pkgVersion = "1.2.2"

        and: "An initialised package cache with a cached file"
        final cachedFile = createCachedFile(testPkg, pkgName, pkgVersion)
        final dpc = new DefaultPackageCache(cachedFile.parentFile)

        expect: "The path to the cached file associated with the given package coordinates"
        dpc.getPackage(pkgName, pkgVersion) == cachedFile
    }

    def "Should determine if a cached file exists for a given package URL"() {
        given: "A test package URI"
        final testPkg = getClass().getResource(testPackageFilename).toURI()

        and: "An initialised package cache with a cached file"
        final cachedFile = createCachedFile(testPkg, testPackageFilename, expected)
        final dpc = new DefaultPackageCache(cachedFile.parentFile)

        expect: "true if the package is cached, otherwise false"
        dpc.hasPackage(testPkg) == expected

        where:
        expected << [true, false]
    }

    def "Should determine if a cached file exists for a given package name and version"() {
        given: "A test package URI"
        final testPkg = getClass().getResource(testPackageFilename).toURI()

        and: "An initialised package cache with a cached file"
        final cachedFile = createCachedFile(testPkg, "ratpack", "1.2.2", createFile)
        final dpc = new DefaultPackageCache(cachedFile.parentFile)

        expect: "The path to the cached file associated with the given package coordinates"
        dpc.hasPackage(pkgName, pkgVersion) == expected

        where:
        pkgName    | pkgVersion  | createFile | expected
        "ratpack"  | "1.2.2"     | true       | true
        "ratpack"  | "1.2.2"     | false      | false
        "ratpack"  | "1.0.1"     | true       | false
        "grails"   | "1.2.2"     | true       | false
    }

    def "Should return a list of cached packages"() {
        given: "A test package URI"
        final testPkg = getClass().getResource(testPackageFilename).toURI()

        and: "An initialised package cache with a cached file"
        for (String v in versions) {
            createCachedFile(testPkg, "ratpack", v, true)
        }
        final cachedFile3 = createCachedFile(testPkg, testPackageFilename, false)
        final dpc = new DefaultPackageCache(cachedFile3.parentFile)

        expect: "The path to the cached file associated with the given package coordinates"
        dpc.listPackages() == expected

        where:
        versions  | createUrlCacheFile | expected
        ["1.2.2"] | false |[new CachedPackage("ratpack", ["1.2.2"])]
        []        | false | []
        ["1.2.2", "1.3.0"] | true | [new CachedPackage("ratpack", ["1.2.2", "1.3.0"]),
                                     new CachedPackage(testPackageFilename, null)]
    }

    private File createCachedFile(URI packageUri, String uriFilename, boolean persistFile = true) {
        final cacheDir = testDir.newFolder()
        final cachedFile = new File(cacheDir, createCachedFilename(packageUri, uriFilename))
        if (persistFile) cachedFile.createNewFile()
        return cachedFile
    }

    private File createCachedFile(
            URI packageUri,
            String packageName,
            String packageVersion,
            boolean persistFile = true) {
        final cacheDir = testDir.newFolder()
        final cachedFile = new File(cacheDir, createCachedFilename(packageUri, packageName, packageVersion))
        if (persistFile) cachedFile.createNewFile()
        return cachedFile
    }

    private String createCachedFilename(URI testPkg, String uriFilename) {
        return "file-${DigestUtils.md5Hex(testPkg.toString())}-${uriFilename}"
    }

    private String createCachedFilename(URI testPkg, String packageName, String packageVersion) {
        return "${packageName}-${packageVersion}.zip"
    }
}
