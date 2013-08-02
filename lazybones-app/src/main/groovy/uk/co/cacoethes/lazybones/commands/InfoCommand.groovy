package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.BintrayPackageSource
import uk.co.cacoethes.lazybones.NoVersionsFoundException
import uk.co.cacoethes.lazybones.PackageInfo

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

        def pkgName = args[0]
        log.info "Fetching package information for '${pkgName}' from Bintray"

        // grab the package from the first repository that has it
        PackageInfo pkgInfo
        try {
            pkgInfo = findPackageInBintrayRepositories(pkgName, config.bintrayRepositories as List<String>)
        }
        catch (NoVersionsFoundException ex) {
            log.severe "No version of '${pkgName}' has been published"
            return 1
        }

        if (!pkgInfo) {
            log.severe "Cannot find a template named '${pkgName}'"
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

    protected PackageInfo findPackageInBintrayRepositories(String pkgName, Collection<String> repositories) {
        for (String bintrayRepoName in repositories) {
            def pkgInfo = new BintrayPackageSource(bintrayRepoName).fetchPackageInfo(pkgName)
            if (pkgInfo) return pkgInfo
        }

        return null
    }
}
