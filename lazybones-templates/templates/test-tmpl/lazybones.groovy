import static uk.co.cacoethes.util.NameType.*

// Specify SCM excludes
scmExclusions "*.iws", "build/", "*.log"

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

testContent << transformText("test-string", from: HYPHENATED, to: CAMEL_CASE) << '\n'
testContent << transformText("ALongName", from: CAMEL_CASE, to: NATURAL) << '\n'

try {
    transformText("test-string", from: HYPHENATED)
}
catch (IllegalArgumentException ex) {
    testContent << "Missing 'to' argument for transformText()"
}

new File(targetDir, "test.txt").text = testContent.toString()

filterProperties.name = "foo"
processTemplates("**/Print*.groovy", filterProperties)