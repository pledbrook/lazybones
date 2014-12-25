package uk.co.cacoethes.lazybones.api

import groovy.transform.ToString
import groovy.transform.TupleConstructor

/**
 * Data object that describes a package, including where it came from and what
 * versions are available for it.
 */
@groovy.transform.Immutable(copyWith = true)
@ToString(includes = ["name", "latestVersion", "owner", "description"])
class PackageInfo {
    /** The source/repository that supplies this template package. */
    PackageSource packageSource

    String name
    String latestVersion

    /**
     * A list of the available versions for this template, including
     * {@link #latestVersion}. The versions are ordered by age, with
     * most recent (latest) first.
     */
    List<String> versions

    /** The person or organisation that publishes this template package. */
    String owner
    String description

    /** A URL where someone can find more information about the template. */
    String infoUrl

    boolean hasAtLeastOneVersion() { !versions.isEmpty() }
    boolean hasVersion(String version) { versions.contains(version) }
}
