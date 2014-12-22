import static uk.co.cacoethes.util.NameType.*

log.warning "User should see this log message"

// Specify SCM excludes
scmExclusions "*.iws", "build/", "*.log"

// Test for regression of #135 - the argument to `ask()` should be coerced to
// a string by default rather than trigger an exception.
def maxThreads = 5
def filterProperties = [:]
filterProperties.group = ask("Define value for 'group': ", null, "group")
filterProperties.version = ask("Define value for 'version' [0.1]: ", "0.1", "version")
filterProperties.maxThreads = ask("Maximum number of threads to use [$maxThreads]: ", "${maxThreads}", "maxThreads")

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

new File(projectDir, "test.txt").text = testContent.toString()

filterProperties.name = "foo"
processTemplates("**/Print*.groovy", filterProperties)
