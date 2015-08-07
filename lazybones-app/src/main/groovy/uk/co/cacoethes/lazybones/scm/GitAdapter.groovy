package uk.co.cacoethes.lazybones.scm

import groovy.util.logging.Log
import org.ini4j.Wini
import uk.co.cacoethes.lazybones.config.Configuration

/**
 * An SCM adapter for git. Make sure that when executing the external processes
 * you use the {@code text} property to ensure that the process output is fully
 * read.
 */
@Log
class GitAdapter implements ScmAdapter {
    private static final String GIT = "git"

    private final String userName
    private final String userEmail

    GitAdapter(Configuration config) {
        // Load the current user's git config if it exists.
        def configFile = new File(System.getProperty("user.home"), ".gitconfig")
        if (configFile.exists()) {
            def ini = new Wini(configFile)
            def userKey = "user"
            userName = ini.get(userKey, "name")
            userEmail = ini.get(userKey, "email")
        }
        else {
            // Use Lazybones config entries if they exist.
            userName = config.getSetting("git.name") ?: "Unknown"
            userEmail = config.getSetting("git.email") ?: "unknown@nowhere.net"
        }
    }

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
        execGit(["init"], location)
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
        def configCmd = "config"
        execGit(["add", "."], location)
        execGit([configCmd, "user.name", userName], location)
        execGit([configCmd, "user.email", userEmail], location)
        execGit(["commit", "-m", message], location)
    }

    /**
     * Executes a git command using an external process. The executable must be
     * on the path! It also logs the output of each command at FINEST level.
     * @param args The git sub-command (e.g. 'status') + its arguments
     * @param location The working directory for the command.
     * @return The return code from the process.
     */
    private int execGit(List args, File location) {
        def process = ([GIT] + args).execute([], location)
        def out = new StringWriter()
        process.consumeProcessOutput out, out
        log.finest out.toString()
        return process.waitFor()
    }
}
