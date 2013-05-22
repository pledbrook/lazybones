package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.BintrayPackageSource

/**
 *
 */
@CompileStatic
@Log
class ListCommand extends AbstractCommand {
    static final String USAGE = """\
USAGE: list
"""

    @Override
    String getName() { return "list" }

    @Override
    String getDescription() {
        return "Lists the templates that are available for you to install."
    }

    @Override
    int execute(List<String> args, Map globalOptions, ConfigObject config) {
        def cmdOptions = parseArguments(args, 0..0)
        if (!cmdOptions) return 1

        for (String bintrayRepoName in config.bintrayRepositories) {
            println "Available templates in ${bintrayRepoName}:"
            println ""

            def pkgSource = new BintrayPackageSource(bintrayRepoName)
            for (name in pkgSource.listPackageNames()) {
                println "    " + name
            }

            println ""
        }

        return 0
    }

    @Override
    protected String getUsage() { return USAGE }
}
