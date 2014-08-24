package uk.co.cacoethes.lazybones.commands

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.PackageNotFoundException

/**
 * Handles the retrieval of template packages from Bintray (or other supported
 * repositories).
 */
@CompileStatic
@Log
class PackageDownloader {

    File downloadPackage(PackageLocation packageLocation, String packageName, String version) {
        def packageFile = new File(packageLocation.cacheLocation)

        if (!packageFile.exists()) {
            packageFile.parentFile.mkdirs()

            // The package info may not have been requested yet. It depends on
            // whether the user specified a specific version or not. Hence we
            // try to fetch the package info first and only throw an exception
            // if it's still null.
            //
            // There is an argument for having getPackageInfo() throw the exception
            // itself. May still do that.
            log.fine "${packageLocation.cacheLocation} is not cached locally. Searching the repositories for it."
            log.fine "Attempting to download ${packageLocation.remoteLocation} into ${packageLocation.cacheLocation}"

            try {
                packageFile.withOutputStream { OutputStream out ->
                    new URL(packageLocation.remoteLocation).withInputStream { InputStream input ->
                        out << input
                    }
                }
            }
            catch (FileNotFoundException ex) {
                packageFile.deleteOnExit()
                throw new PackageNotFoundException(packageName, version, ex)
            }
            catch (all) {
                packageFile.deleteOnExit()
                throw all
            }
        }

        return packageFile

    }
}
