package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import joptsimple.OptionSet
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
    protected String getUsage() { return USAGE }

    @Override
    protected IntRange getParameterRange() {
        return 1..1
    }

    @Override
    int doExecute(OptionSet cmdOptions,  Map globalOptions, ConfigObject config) {
        String packageName = cmdOptions.nonOptionArguments()[0]

        log.info "Fetching package information for '${packageName}' from Bintray"

        // grab the package from the first repository that has it
        def pkgInfo
        config.bintrayRepositories.find { String bintrayRepoName ->
            def pkgSource = new BintrayPackageSource(bintrayRepoName)
            pkgInfo = pkgSource.fetchPackageInfo(packageName)
            pkgInfo
        }

        if (!pkgInfo) {
            println "Cannot find a template named '${packageName}'"
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


}
