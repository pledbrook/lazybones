package uk.co.cacoethes.lazybones

class LazybonesScriptFunctionalSpec extends AbstractFunctionalSpec {

    def "lazybones is deleted after package is installed"() {
        when: "I run lazybones with the create command for the groovy-gradle template"
        def exitCode = runCommand(["create", "groovy-app", "0.2", "groovyapp"], baseWorkDir, ["foo", "0.1"])

        then: "It successfully completes, deleting the lazybones script after unpacking the template"
        exitCode == 0
        def appDir = new File(baseWorkDir, "groovyapp")
        !new File(appDir, "lazybones.groovy").exists()

        and: "It runs the post install script, substituting in the given group and version into the build file"
        def text = new File(appDir, "build.gradle").text
        text.contains("group = \"foo\"")
        text.contains("version = \"0.1\"")
    }

    def "lazybones passes commandline P args to the root script"() {
        when: "creating a groovyapp with all options passed in"
        def exitCode = runCommand(["--stacktrace", "create", "groovy-app", "0.2", "groovyappWithArgs", "-Pversion=0.2", "-Pgroup=bar"], baseWorkDir)

        then: "It successfully completes"
        exitCode == 0

        and: "the gradle build file contains proper group and version"
        def appDir = new File(baseWorkDir, "groovyappWithArgs")
        def text = new File(appDir, "build.gradle").text
        text.contains("group = \"bar\"")
        text.contains("version = \"0.2\"")
    }
}
