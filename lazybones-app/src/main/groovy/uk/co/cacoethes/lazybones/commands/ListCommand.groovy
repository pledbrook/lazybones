package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import joptsimple.OptionParser
import joptsimple.OptionSet
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.lazybones.packagesources.BintrayPackageSource
import wslite.http.HTTPClientException

import java.util.logging.Level
import java.util.regex.Pattern

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
    private static final String CACHED_OPTION = "cached"
    private static final String SUBTEMPLATES_OPTION = "subs"
    private static final int PADDING = 30

    private static final String VERSION_PATTERN = /\d+\.\d[^-]*(?:-SNAPSHOT)?/

    File cacheDir

    ListCommand(Configuration config) {
        this.cacheDir = config.getSetting("cache.dir") as File
    }

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
    protected int doExecute(OptionSet optionSet, Map globalOptions, Configuration config) {

        def remoteTemplates = fetchRemoteTemplates(config.getSetting("bintrayRepositories"))

        boolean offline = false
        if (!optionSet.hasOptions()) {
            offline = handleRemoteTemplates(remoteTemplates, globalOptions.stacktrace as boolean)
        }

        if (optionSet.has(CACHED_OPTION) || offline) handleCachedTemplates(cacheDir)
        if (!optionSet.has(SUBTEMPLATES_OPTION)) handleMappings(config.getSubSettings("templates.mappings"))
        if (optionSet.has(SUBTEMPLATES_OPTION)) return handleSubtemplates()

        return 0
    }

    protected OptionParser doAddToParser(OptionParser parser) {
        parser.accepts(CACHED_OPTION, "Lists the cached templates instead of the remote ones.")
        parser.accepts(SUBTEMPLATES_OPTION, "Lists any subtemplates in the current project.")
        return parser
    }

    protected static boolean handleRemoteTemplates(Map<String, Object> remoteTemplates, boolean stacktrace) {
        boolean offline = remoteTemplates.every { k, v -> OfflineMode.isOffline(v) }

        if (offline) {
            OfflineMode.printlnOfflineMessage(remoteTemplates.findResult { k, v -> v }, log, stacktrace)
        }
        else {
            printDetailsForRemoteTemplates(remoteTemplates, stacktrace)
        }
        return offline
    }

    @SuppressWarnings("SpaceBeforeOpeningBrace")
    protected static void handleCachedTemplates(File cacheDir) {
        println "Cached templates"
        println()

        def templateNamePattern = ~/^(.*)-($VERSION_PATTERN)\.zip$/

        def templates = findMatchingTemplates(cacheDir, templateNamePattern).groupBy { File f ->
            templateNamePattern.matcher(f.name)[0][1]
        }.collectEntries { String tmplName, List<File> files ->
            // Extract the version numbers and make those the key value.
            [ tmplName, files.collect { templateNamePattern.matcher(it.name)[0][2] } ]
        }

        for (entry in templates) {
            println INDENT + entry.key.padRight(PADDING) + entry.value.sort().reverse().join(", ")
        }

        println()
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
        println "Available templates in ${repoName}"
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

    @SuppressWarnings("DuplicateNumberLiteral")
    protected static void handleMappings(Map mappings) {
        if (mappings) {
            println "Available mappings"
            println()

            int maxKeySize = mappings.keySet().inject(0) { int max, String key -> Math.max(key.size(), max) }

            mappings.each { String key, value ->
                println INDENT + key.padRight(maxKeySize + 2) + "-> " + value
            }

            println()
        }
    }

    /**
     * Handle local subtemplates only
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    protected static int handleSubtemplates() {
        // is the current dir a project created by Lazybones?
        File lazybonesDir = new File('.lazybones')
        if (!lazybonesDir.exists()) {
            println "You can only use --subs in a Lazybones-created project directory"
            return 1   // Error exit code
        }

        def templateNamePattern = ~/^(.*)-template-($VERSION_PATTERN)\.zip$/
        def templates = findMatchingTemplates(lazybonesDir, templateNamePattern)

        // are there any templates available?
        if (templates) {
            println "Available subtemplates"
            println()

            for (f in templates) {
                def matcher = templateNamePattern.matcher(f.name)
                println INDENT + matcher[0][1].padRight(PADDING) + matcher[0][2]
            }

            println()
        }
        else {
            println "This project has no subtemplates"
        }

        return 0    // 'No error' exit code
    }

    private static File[] findMatchingTemplates(File dir, Pattern pattern) {
        dir.listFiles( { File f ->
            pattern.matcher(f.name).matches()
        } as FileFilter).sort { it.name }
    }
}
