package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.apache.commons.io.FileUtils
import org.junit.*

class ListFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()
    def remoteTemplates = ["afterburnerfx", "dropwizard", "gaelyk", "gradle-plugin", "gradle-quickstart"]

    void setup() {
        initProxy(recorder.proxy.address())
    }

    @Betamax(tape='list-tape')
    def "list command prints all available packages"() {
        when: "I run lazybones with the list command"
        def exitCode = runCommand(["list"], baseWorkDir)

        then: "It displays the available packages as a list"
        exitCode == 0
        output =~ /(?m)^Available mappings\s+/ +
                /\s+customRatpack  -> http:\/\/dl.dropboxusercontent.com\/u\/29802534\/custom-ratpack.zip\s+/ +
                /\s+doesNotExist   -> file:\/\/\/does\/not\/exist\s+/
        output =~ /\s+${remoteTemplates.join('\\s+')}\s+/
        !(output =~ /Exception/)
        !(output =~ /Cached templates/)
        !(output =~ /Available subtemplates/)
    }

    @Betamax(tape='list-tape')
    def "list command prints cached templates"() {
        given: "A template in the cache matching a remote one"
        def versions = addMultiVersionCachedPackage("ratpack")

        when: "I run the list command with --cached option"
        def exitCode = runCommand(["list", "--cached"], baseWorkDir)

        then: "It displays the available packages as a list"
        exitCode == 0
        output =~ /(?m)^Available mappings\s+/ +
                /\s+customRatpack  -> http:\/\/dl.dropboxusercontent.com\/u\/29802534\/custom-ratpack.zip\s+/ +
                /\s+doesNotExist   -> file:\/\/\/does\/not\/exist\s+/

        output =~ /(?m)^Cached templates\s+/ +
                /Oops-stuff                    1.0.4\s+/ +
                /ratpack                       ${versions.join(', ')}\s+/ +
                /subtemplates-tmpl             0.1\s+/ +
                /test-handlebars               0.1.1/
        !(output =~ /Exception/)
        !(remoteTemplates.any { it in output })
        !(output =~ /Available subtemplates/)
    }

    @Betamax(tape='list-tape')
    def "list command with --subs in non-project directory"() {
        when: "I run the list command with --subs option"
        // do not clear workdir or we lose our setup
        def exitCode = runCommand(["list", "--subs"], baseWorkDir, [], false)

        then: "It displays an error message"
        exitCode != 0
        output =~ /You can only use --subs in a Lazybones-created project directory/
        !(output =~ /(?m)^Available subtemplates\s+/)
        !(output =~ /Exception/)
        !(output =~ /Cached templates/)
        !(output =~ /Available mappings/)
        !(remoteTemplates.any { it in output })
    }

    @Betamax(tape='list-tape')
    def "list command prints no subtemplates"() {
        given: "A lazybones generated project"
        def projectDir = createTempProject()

        when: "I run the list command with --subs option"
        // do not clear workdir or we lose our setup
        def exitCode = runCommand(["list", "--subs"], projectDir, [], false)

        then: "It does not display any available subtemplates"
        exitCode == 0
        output =~ /This project has no subtemplates/
        !(output =~ /(?m)^Available subtemplates\s+/)
        !(output =~ /Exception/)
        !(output =~ /Cached templates/)
        !(output =~ /Available mappings/)
        !(remoteTemplates.any { it in output })
    }

    @Betamax(tape='list-tape')
    def "list command prints available subtemplates"() {
        given: "A lazybones generated project"
        def projectDir = createTempProject()

        and: "A subtemplate is available in the local lazybones directory"
        addSubtemplate(projectDir, "artifact", "1.0.0")
        addSubtemplate(projectDir, "web", "0.8")

        when: "I run the list command with --subs option"
        // do not clear workdir or we loose our setup
        def exitCode = runCommand(["list", "--subs"], projectDir, [], false)

        then: "It does display all available subtemplates"
        exitCode == 0
        output =~ /(?m)^Available subtemplates\s+/ +
                  /^\s+artifact\s+1.0.0\s+/ +
                  /^\s+web\s+0.8\s+/
        !(output =~ /Exception/)
        !(output =~ /Cached templates/)
        !(output =~ /Available mappings/)
        !(remoteTemplates.any { it in output })
    }

    private File createTempProject() {
        final projectDir = new File(baseWorkDir, 'project')
        new File(projectDir, '.lazybones').mkdirs()

        filesToDelete << projectDir
        return projectDir
    }

    private List addMultiVersionCachedPackage(String pkgName) {
        def versions = ["0.9.18", "0.1"]
        for (String version in versions) {
            def pkgFile = new File(cacheDirPath, "${pkgName}-${version}.zip")
            if (!pkgFile.exists()) {
                FileUtils.touch(pkgFile)
                filesToDelete << pkgFile
            }
        }

        return versions
    }

    private File addSubtemplate(File projectDir, String name, String version) {
        final templateFile = new File(new File(projectDir, ".lazybones"), "${name}-template-${version}.zip")
        FileUtils.touch(templateFile)
        return templateFile
    }
}
