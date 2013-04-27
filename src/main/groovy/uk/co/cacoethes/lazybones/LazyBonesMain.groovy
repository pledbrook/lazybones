package uk.co.cacoethes.lazybones

import uk.co.cacoethes.util.ArchiveMethods
import wslite.rest.*

@groovy.transform.CompileStatic
class LazyBonesMain {

    static final String templatesBaseUrl = "http://dl.bintray.com/v1/content/pledbrook/lazybones-templates"
    static final File cacheDir = new File(System.getProperty('user.home'), ".groovy/lazybones-templates")

    static void main(String[] args) {

        /*
        def response = client.get(path:'/users/show.json', query:[screen_name:'jwagenleitner', include_entities:true])

        assert 200 == response.statusCode
        assert "John Wagenleitner" == response.json.name
        */


        // REST API currently requires an API key, which I'm not keen on making public!
        //def restClient = new RESTClient("https://bintray.com/api/v1")

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
        if (args.size() != 3) {
            log """\
Incorrect number of arguments.

USAGE: create <template> <version> <dir>

  where  template = The name of the project template to use.
         version  = The version of the project template to use.
         dir      = The name of the directory in which to create the project
                    structure. This can be '.' to mean 'in the current
                    directory.'
"""
            return 1
        }

        // Can't fetch the latest version until BinTray allows anonymous API access.
        // Or I set up a separate server for this stuff.
        def templateZip = fetchTemplate(args[0], args[1]) 
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
        log "Sorry, this command isn't implemented yet."
        return 1
    }

    static int helpCommand(List<String> args) {

        final cmdDescriptions = [
            create: "Creates a new project structure from a named template."] as Map<String, String>//,
            // 'list' is unavailable until get anonymous BinTray API access
    //        list: "Lists the available project templates." ]

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
        def packageFile = new File(cacheDir, "${name}-${version}.zip")
        if (!packageFile.exists()) {
            cacheDir.mkdirs()

            packageFile.withOutputStream { OutputStream out ->
                new URL(templatesBaseUrl + "/${name}-template-${version}.zip").withInputStream { InputStream input ->
                    out << input
                }
            }
        }

        return packageFile

    }

    private static void log(String message = "") {
        System.out.println message
    }
    
}
