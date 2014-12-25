package uk.co.cacoethes.lazybones.api

/**
 * Created by pledbrook on 23/01/2016.
 */
@groovy.transform.Immutable(copyWith = true)
class CachedPackage {
    String name
    String version
    String sourceRepoName
    String sourceUrl
}
