package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.BintrayPackageSource

/**
 *
 */
@CompileStatic
@Log
class InfoCommand extends AbstractCommand {
    static final String USAGE = """\
USAGE: info <template>

  where  template = The name of the project template you want information
                    about
"""

    @Override
    String getName() { return "info" }

    @Override
    String getDescription() {
        return "Displays information about a template, such as latest version, description, etc."
    }

    @Override
    int execute(List<String> args, Map globalOptions, ConfigObject config) {
        def cmdOptions = parseArguments(args, 1..1)
        if (!cmdOptions) return 1

        log.info "Fetching package information for '${args[0]}' from Bintray"

        // grab the package from the first repository that has it
        def pkgInfo
        config.bintrayRepositories.find { String bintrayRepoName ->
            def pkgSource = new BintrayPackageSource(bintrayRepoName)
            pkgInfo = pkgSource.fetchPackageInfo(args[0])
            pkgInfo
        }

        if (!pkgInfo) {
            println "Cannot find a template named '${args[0]}'"
            return 1
        }

        println "Name:        " + pkgInfo.name
        println "Latest:      " + pkgInfo.latestVersion
        if (pkgInfo.description) println "Description: " + pkgInfo.description
        if (pkgInfo.owner) println "Owner:       " + pkgInfo.owner
        println "Versions:    " + pkgInfo.versions.join(", ")

        if (pkgInfo.url) {
            println ""
            println "More information at " + pkgInfo.url
        }
        return 0
    }

    @Override
    protected String getUsage() { return USAGE }
}
