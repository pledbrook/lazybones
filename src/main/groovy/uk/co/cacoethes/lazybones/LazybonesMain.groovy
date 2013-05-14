package uk.co.cacoethes.lazybones

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.codehaus.groovy.control.CompilerConfiguration
import uk.co.cacoethes.util.ArchiveMethods

import java.util.logging.LogManager

@CompileStatic
@Log
class LazybonesMain {

    static final String templatesBaseUrl = "http://dl.bintray.com/v1/content/pledbrook/lazybones-templates"
    static final File installDir = new File(System.getProperty('user.home'), ".lazybones/templates")

    static void main(String[] args) {
        initLogging()

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
                help: this.&helpCommand] as Map<String, Closure>

        def cmdClosure = cmdMap[cmd]
        if (!cmdClosure) {
            println "No command '" + cmd + "'"
            System.exit 1
        }

        int retval = (int) cmdClosure.call(argsList)
        System.exit retval
    }

    static int createCommand(List<String> args) {
        if (args.size() < 2 || args.size() > 3) {
            println """\
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
                println "Cannot find a template named '${args[0]}'. Project has not been created."
                return 1
            }

            args.add(1, pkgInfo.latestVersion)
        }

        // Can't fetch the latest version until Bintray allows anonymous API access.
        // Or I set up a separate server for this stuff.
        def templateZip = fetchTemplate(args[0], args[1])

        if (!templateZip) {
            println "Cannot find version ${args[1]} of template '${args[0]}'. Project has not been created."
            return 1
        }

        def targetDir = new File(args[2])
        targetDir.mkdirs()
        ArchiveMethods.unzip(templateZip, targetDir)

        try {
            runPostInstallScript(targetDir)
        }
        catch (Throwable throwable) {
            //TODO: once we support --stacktrace, we should handle this better
            log.warning("lazybones script caused an exception, project might be corrupted")
            log.throwing(LazybonesMain.name, "runPostInstallScript", throwable)
            return 1
        }

        // Find a suitable README and display that if it exists.
        def readmeFiles = targetDir.listFiles({ File dir, String name ->
            name == "README" || name.startsWith("README")
        } as FilenameFilter)

        println ""
        if (!readmeFiles) println "This project has no README"
        else println readmeFiles[0].text

        println ""
        println "Project created in ${args[2]}!"

        return 0
    }

    /**
     *
     * @param targetDir the target directory that contains the lazybones.groovy script
     * @return the lazybones script if it exists
     */
    static private Script runPostInstallScript(File targetDir) {
        def file = new File(targetDir, "lazybones.groovy")
        if (file.exists()) {
            def compiler = new CompilerConfiguration()
            compiler.setScriptBaseClass(LazybonesScript.class.name)
            def shell = new GroovyShell(this.class.classLoader, new Binding(), compiler)

            LazybonesScript script = shell.parse(file) as LazybonesScript
            script.setTargetDir(targetDir.path)
            script.run()
            file.delete()
            return script
        }

        return null
    }

    static int listCommand(List<String> args) {
        println "Available templates:"
        println ""

        def pkgSource = new BintrayPackageSource()
        for (name in pkgSource.listPackageNames()) {
            println "    " + name
        }
        return 0
    }

    static int infoCommand(List<String> args) {
        if (args.size() != 1) {
            println """\
Incorrect number of arguments.

USAGE: info <template>

  where  template = The name of the project template you want information
                    about
"""
            return 1
        }

        log.info "Fetching package information for '${args[0]}' from Bintray"

        def pkgSource = new BintrayPackageSource()
        def pkgInfo = pkgSource.fetchPackageInfo(args[0])

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

    static int helpCommand(List<String> args) {

        final cmdDescriptions = [
                create: "Creates a new project structure from a named template.",
                list: "Lists the available project templates.",
                info: "Displays information about a given template."] as Map<String, String>

        println "Lazybones is a command-line based tool for creating basic software projects from templates."
        println ""
        println "Available commands:"
        println ""
        cmdDescriptions.each { String cmd, String desc ->
            println "    " + cmd.padRight(15) + desc
        }
        println ""

        return 0
    }

    private static File fetchTemplate(String name, String version) {
        // Does it exist in the cache? If not, pull it from Bintray.
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
                println "${externalUrl} was not found."
                packageFile.deleteOnExit()
                return null
            }
        }

        return packageFile

    }

    private static void initLogging() {
        def inputStream = new ByteArrayInputStream(LOG_CONFIG.getBytes("UTF-8"))
        LogManager.logManager.readConfiguration(inputStream)
    }

    /**
     * Logging configuration in Properties format. It simply sets up the console
     * handler with a formatter that just prints the message without any decoration.
     */
    private static final String LOG_CONFIG = """\
# Logging
handlers = java.util.logging.ConsoleHandler

# Console logging
java.util.logging.ConsoleHandler.formatter = uk.co.cacoethes.util.PlainFormatter
java.util.logging.ConsoleHandler.level = WARNING
"""
}
