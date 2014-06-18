package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import joptsimple.OptionSet
import uk.co.cacoethes.lazybones.packagesources.BintrayPackageSource
import wslite.http.HTTPClientException

import java.util.logging.Level

/**
 * A Lazybones command that prints out all the available templates by name,
 * including any aliases that the user has configured in his or her settings.
 */
@Log
class ListCommand extends AbstractCommand {
    static final String USAGE = """\
USAGE: list
"""

    private static final String INDENT = "    "

    @Override
    String getName() { return "list" }

    @Override
    String getDescription() {
        return "Lists the templates that are available for you to install."
    }

    @Override
    protected IntRange getParameterRange() {
        0..0
    }

    @Override
    protected String getUsage() { return USAGE }

    @Override
    protected int doExecute(OptionSet optionSet, Map globalOptions, ConfigObject config) {

        def remoteTemplates = fetchRemoteTemplates(config.bintrayRepositories)

        handleRemoteTemplates(remoteTemplates, globalOptions.stacktrace as boolean)
        handleMappings(config.templates.mappings)

        return 0
    }

    protected static void handleRemoteTemplates(Map<String, Object> remoteTemplates, boolean stacktrace) {
        boolean offline = remoteTemplates.every { k, v -> OfflineMode.isOffline(v) }

        if (offline) {
            OfflineMode.printlnOfflineMessage(remoteTemplates.findResult { k, v -> v }, log, stacktrace)
        }
        else {
            printDetailsForRemoteTemplates(remoteTemplates, stacktrace)
        }
    }

    protected static void printDetailsForRemoteTemplates(Map<String, Object> remoteTemplates, boolean stacktrace) {
        for (repoEntry in remoteTemplates) {
            printDetailsForRemoteRepository(repoEntry.key, repoEntry.value, stacktrace)
        }
    }

    protected static void printDetailsForRemoteRepository(String repoName, Exception ex, boolean stacktrace) {
        println "Can't connect to ${repoName}: ${ex.message}"
        if (stacktrace) log.log Level.WARNING, "", ex
        println()
    }

    @SuppressWarnings("UnusedMethodParameter")
    protected static void printDetailsForRemoteRepository(
            String repoName,
            Collection<String> templateNames,
            boolean stacktrace) {
        println "Available templates in ${repoName}:"
        println()

        for (name in templateNames.sort()) {
            println INDENT + name
        }

        println()
    }

    protected Map<String, Object> fetchRemoteTemplates(Collection<String> bintrayRepositories) {
        bintrayRepositories.collectEntries { String repoName ->
            [repoName, fetchPackageNames(repoName)]
        }
    }

    protected fetchPackageNames(String repoName) {
        try {
            def pkgSource = new BintrayPackageSource(repoName)
            return pkgSource.listPackageNames().sort()
        }
        catch (HTTPClientException ex) {
            return ex.cause
        }
    }

    protected static void handleMappings(Map mappings) {
        if (mappings) {
            println "Available mappings"
            println ""

            int maxKeySize = mappings.keySet().inject(0) { int max, String key -> Math.max(key.size(), max) }

            mappings.each { String key, value ->
                println INDENT + key.padRight(maxKeySize + 2) + "-> " + value
            }

            println ""
        }
    }
}
