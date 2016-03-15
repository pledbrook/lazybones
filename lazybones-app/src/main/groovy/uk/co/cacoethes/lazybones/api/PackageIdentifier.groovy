package uk.co.cacoethes.lazybones.api

import groovy.transform.Immutable

/**
 * Created by pledbrook on 10/03/2016.
 */
@Immutable(copyWith = true)
class PackageIdentifier {
    String repoName
    String name
    String version
}
