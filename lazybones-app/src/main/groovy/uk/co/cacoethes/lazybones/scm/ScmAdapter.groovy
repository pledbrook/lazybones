package uk.co.cacoethes.lazybones.scm

/**
 * An adapter into different Source Control Management (SCM) systems. This
 * interface currently assumes that the SCM is a distributed one, like git.
 */
interface ScmAdapter {
    /**
     * Returns the name of the file that contains the exclusions for this SCM,
     * e.g. ".gitignore" for git.
     */
    String getExclusionsFilename()

    /**
     * Creates a new local repository in the given location.
     */
    void initializeRepository(File location)

    /**
     * Adds and commits all the files in the given location. It should take
     * account of any exclusions file in the directory.
     */
    void commitInitialFiles(File location, String message)
}
