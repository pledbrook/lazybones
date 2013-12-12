package uk.co.cacoethes.lazybones.scm

import groovy.transform.CompileStatic
import groovy.util.logging.Log

/**
 * An SCM adapter for git. Make sure that when executing the external processes
 * you use the {@code text} property to ensure that the process output is fully
 * read.
 */
@CompileStatic
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
        def output = [GIT, "init"].execute([], location).text
        log.finest output
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
        def output = [GIT, "add", "."].execute([], location).text
        log.finest output

        output = [GIT, "commit", "-m", message].execute([], location).text
        log.finest output
    }
}
