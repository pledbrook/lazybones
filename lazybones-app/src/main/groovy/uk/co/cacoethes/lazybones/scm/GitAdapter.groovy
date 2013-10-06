package uk.co.cacoethes.lazybones.scm

/**
 * An SCM adapter for git.
 */
class GitAdapter implements ScmAdapter {
    @Override
    String getExclusionsFilename() {
        return ".gitignore"
    }

    /**
     * Creates a new git repository in the given location by spawning an
     * external {@code git init} command.
     */
    @Override
    void createRepository(File location) {
        ["git", "init"].execute([], location)
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
        ["git", "add", "."].execute([], location)
        ["git", "commit", "-m", message].execute([], location)
    }
}
