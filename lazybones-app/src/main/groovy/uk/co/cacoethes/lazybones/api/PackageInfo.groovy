package uk.co.cacoethes.lazybones.api

/**
 * Data object that describes a package, including where it came from and what
 * versions are available for it.
 */
@groovy.transform.Canonical
class PackageInfo {
    PackageSource packageSource
    String name
    String latestVersion
    List<String> versions
    String owner
    String description
    String infoUrl

    boolean hasAtLeastOneVersion() { versions as boolean }
    boolean hasVersion(String version) { versions.contains(version) }
}
