package uk.co.cacoethes.lazybones.scm

import groovy.util.logging.Log

/**
 * An SCM adapter for git.
 */
@Log
class GitAdapter implements ScmAdapter {
    private static final String GIT = "git"

    @Override
    String getExclusionsFilename() {
        return ".gitignore"
    }

    /**
     * Creates a new git repository in the given location by spawning an
     * external {@code git init} command.
     */
    @Override
    void initializeRepository(File location) {
        [GIT, "init"].execute([], location)
    }

    /**
     * Adds the initial files in the given location and commits them to the
     * git repository.
     * @param location The location of the git repository to commit the files
     * in.
     * @param message The commit message to use.
     */
    @Override
    void commitInitialFiles(File location, String message) {
        [GIT, "add", "."].execute([], location)
        [GIT, "commit", "-m", message].execute([], location)
    }
}
