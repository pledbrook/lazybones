package uk.co.cacoethes.lazybones

import uk.co.cacoethes.lazybones.packagesources.PackageSource

/**
 * Data class representing metadata about a Lazybones package.
 */
@groovy.transform.Canonical
class PackageInfo {
    PackageSource source
    String name
    String latestVersion
    List<String> versions
    String owner
    String description
    String url

    Boolean hasVersion() {
        versions as boolean
    }
}
