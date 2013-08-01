def filterProperties = [:]
filterProperties.group = ask("Define value for 'group': ", null, "group")
filterProperties.version = ask("Define value for 'version' [0.1]: ", "0.1", "version")

processTemplates("build.gradle", filterProperties)

def testContent = new StringBuilder()
testContent << "Version: " << lazybonesVersion << '\n'

// Tests that the versions are provided as integers.
assert lazybonesMajorVersion instanceof Number
assert lazybonesMinorVersion instanceof Number

if (lazybonesMajorVersion == 0 && lazybonesMinorVersion < 4) {
    testContent << "Your Lazybones version is too old\n"
}
else {
    testContent << "Your Lazybones version is OK - you're good to go!\n"
}

new File(targetDir, "test.txt").text = testContent.toString()

filterProperties.name = "foo"
processTemplates("**/Print*.groovy", filterProperties)