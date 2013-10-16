package uk.co.cacoethes.lazybones

import uk.co.cacoethes.lazybones.packagesources.PackageSource

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
