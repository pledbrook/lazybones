package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import joptsimple.OptionSet
import uk.co.cacoethes.lazybones.packagesources.BintrayPackageSource

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

        handleMappings(config.templates.mappings)

        for (String bintrayRepoName in config.bintrayRepositories) {
            println "Available templates in ${bintrayRepoName}:"
            println ""

            def pkgSource = new BintrayPackageSource(bintrayRepoName)
            for (name in pkgSource.listPackageNames()) {
                println INDENT + name
            }

            println ""
        }

        return 0
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
