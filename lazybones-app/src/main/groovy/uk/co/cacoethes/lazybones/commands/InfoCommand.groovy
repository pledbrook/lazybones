package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import joptsimple.OptionSet
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.lazybones.packagesources.BintrayPackageSource
import uk.co.cacoethes.lazybones.NoVersionsFoundException
import uk.co.cacoethes.lazybones.PackageInfo
import wslite.http.HTTPClientException

import java.util.logging.Level

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
    int doExecute(OptionSet cmdOptions,  Map globalOptions, Configuration config) {
        String packageName = cmdOptions.nonOptionArguments()[0]

        log.info "Fetching package information for '${packageName}' from Bintray"

        // grab the package from the first repository that has it
        PackageInfo pkgInfo
        try {
            pkgInfo = findPackageInBintrayRepositories(
                    packageName,
                    config.getSetting("bintrayRepositories") as List<String>)
        }
        catch (NoVersionsFoundException ex) {
            log.severe "No version of '${packageName}' has been published"
            return 1
        }
        catch (HTTPClientException ex) {
            if (OfflineMode.isOffline(ex)) {
                OfflineMode.printlnOfflineMessage(ex, log, globalOptions.stacktrace as boolean)
            }
            else {
                log.severe "Unexpected failure: ${ex.message}"
                if (globalOptions.stacktrace) log.log Level.SEVERE, "", ex
            }

            println()
            println "Cannot fetch package info"
            return 1
        }
        catch (all) {
            log.severe "Unexpected failure: ${all.message}"
            if (globalOptions.stacktrace) log.log Level.SEVERE, "", all
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

    protected PackageInfo findPackageInBintrayRepositories(String pkgName, Collection<String> repositories) {
        for (String bintrayRepoName in repositories) {
            def pkgInfo = new BintrayPackageSource(bintrayRepoName).fetchPackageInfo(pkgName)
            if (pkgInfo) return pkgInfo
        }

        return null
    }
}
