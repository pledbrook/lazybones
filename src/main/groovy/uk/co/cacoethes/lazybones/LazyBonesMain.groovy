package uk.co.cacoethes.lazybones

import uk.co.cacoethes.util.ArchiveMethods

@groovy.transform.CompileStatic
class LazybonesMain {

    static final String templatesBaseUrl = "http://dl.bintray.com/v1/content/pledbrook/lazybones-templates"
    static final File installDir = new File(System.getProperty('user.home'), ".lazybones/templates")

    static void main(String[] args) {
        String cmd
        List argsList = args as List
        if (argsList.size() == 0) {
            cmd = "help"
            argsList = []
        }
        else {
            cmd = argsList[0]
            argsList = argsList.size() == 1 ? [] : argsList[1..-1]
        }

        // Execute the corresponding command
        def cmdMap = [
            create: this.&createCommand,
            list: this.&listCommand,
            info: this.&infoCommand,
            help: this.&helpCommand ] as Map<String, Closure>

        def cmdClosure = cmdMap[cmd]
        if (!cmdClosure) {
            log "No command '" + cmd + "'"
            System.exit 1
        }

        int retval = (int) cmdClosure.call(argsList)
        System.exit retval
    }

    static int createCommand(List<String> args) {
        if (args.size() < 2 || args.size() > 3) {
            log """\
Incorrect number of arguments.

USAGE: create <template> <version>? <dir>

  where  template = The name of the project template to use.
         version  = (optional) The version of the project template to use. Uses
                    the latest version of the template by default.
         dir      = The name of the directory in which to create the project
                    structure. This can be '.' to mean 'in the current
                    directory.'
"""
            return 1
        }

        // Inject the latest version into the args list if the user hasn't
        // provided it.
        if (args.size() == 2) {
            def pkgSource = new BintrayPackageSource()
            def pkgInfo = pkgSource.fetchPackageInfo(args[0])

            if (!pkgInfo) {
                log "Cannot find a template named '${args[0]}'. Project has not been created."
                return 1
            }

            args.add(1, pkgInfo.latestVersion)
        }

        // Can't fetch the latest version until BinTray allows anonymous API access.
        // Or I set up a separate server for this stuff.
        def templateZip = fetchTemplate(args[0], args[1])

        if (!templateZip) {
            log "Cannot find version ${args[1]} of template '${args[0]}'. Project has not been created."
            return 1
        }

        def targetDir = new File(args[2])
        targetDir.mkdirs()
        ArchiveMethods.unzip(templateZip, targetDir)

        // Find a suitable README and display that if it exists.
        def readmeFiles = targetDir.listFiles( { File dir, String name ->
            name == "README" || name.startsWith("README")
        } as FilenameFilter)

        log()
        if (!readmeFiles) log "This project has no README"
        else log readmeFiles[0].text

        log()
        log "Project created in ${args[2]}!"

        return 0
    }

    static int listCommand(List<String> args) {
        log "Available templates:"
        log ""

        def pkgSource = new BintrayPackageSource()
        for (name in pkgSource.listPackageNames()) {
            log "    " + name
        }
        return 0
    }

    static int infoCommand(List<String> args) {
        if (args.size() != 1) {
            log """\
Incorrect number of arguments.

USAGE: info <template>

  where  template = The name of the project template you want information
                    about
"""
            return 1
        }

        def pkgSource = new BintrayPackageSource()
        def pkgInfo = pkgSource.fetchPackageInfo(args[0])

        if (!pkgInfo) {
            log "Cannot find a template named '${args[0]}'"
            return 1
        }

        log "Name:        " + pkgInfo.name
        log "Latest:      " + pkgInfo.latestVersion
        if (pkgInfo.description) log "Description: " + pkgInfo.description
        if (pkgInfo.owner) log "Owner:       " + pkgInfo.owner
        log "Versions:    " + pkgInfo.versions.join(", ")

        if (pkgInfo.url) {
            log ""
            log "More information at " + pkgInfo.url
        }
        return 0
    }

    static int helpCommand(List<String> args) {

        final cmdDescriptions = [
            create: "Creates a new project structure from a named template.",
            list: "Lists the available project templates.",
            info: "Displays information about a given template." ] as Map<String, String>

        log "Lazybones is a command-line based tool for creating basic software projects from templates."
        log()
        log "Available commands:"
        log()
        cmdDescriptions.each { String cmd, String desc ->
            log "    " + cmd.padRight(15) + desc
        }
        log()

        return 0
    }

    private static File fetchTemplate(String name, String version) {
        // Does it exist in the cache? If not, pull it from BinTray.
        def packageFile = new File(installDir, "${name}-${version}.zip")

        if (!packageFile.exists()) {
            installDir.mkdirs()
            String externalUrl = templatesBaseUrl + "/${name}-template-${version}.zip"
            try {
                packageFile.withOutputStream { OutputStream out ->
                    new URL(externalUrl).withInputStream { InputStream input ->
                        out << input
                    }
                }
            }
            catch (FileNotFoundException fileNotFoundException) {
                log "${externalUrl} was not found."
                packageFile.deleteOnExit()
                return null
            }
        }

        return packageFile

    }

    private static void log(String message = "") {
        System.out.println message
    }
    
}
