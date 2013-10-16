package uk.co.cacoethes.lazybones.commands

import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.PackageNotFoundException

@Log
class PackageDownloader {
    /** Where the template packages are stored/cached */
    static final File INSTALL_DIR = new File(System.getProperty('user.home'), ".lazybones/templates")

    File downloadPackage(PackageLocation packageLocation, CreateCommandInfo info) {
        def packageFile = new File(packageLocation.cacheLocation)

        if (!packageFile.exists()) {
            INSTALL_DIR.mkdirs()

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
                throw new PackageNotFoundException(info.packageName, info.requestedVersion, ex)
            }
            catch (all) {
                packageFile.deleteOnExit()
                throw all
            }
        }

        return packageFile

    }
}
