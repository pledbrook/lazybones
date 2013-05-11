package uk.co.cacoethes.lazybones

class LazybonesScriptSpec extends AbstractFunctionalSpec {

    def "lazybones is deleted after package is installed"() {
        when: "I run lazybones with the create command for the groovy-gradle template"
        def exitCode = runCommand(["create", "groovy-gradle", "0.1", "groovyapp"], baseWorkDir, ["foo", "0.1"])

        then: "It deletes the lazybones script after unpacking template and running lazybones.groovy and files are filtered"
        exitCode == 0
        def appDir = new File(baseWorkDir, "groovyapp")
        !new File(appDir, "lazybones.groovy").exists()
        def text = new File(appDir, "build.gradle").text
        text.contains("group = \"foo\"")
        text.contains("version = \"0.1\"")
    }
}