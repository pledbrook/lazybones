package uk.co.cacoethes.lazybones

@groovy.transform.Canonical
class PackageInfo {
    PackageSource source
    String name
    String latestVersion
    List<String> versions
    String owner
    String description
    String url
}
