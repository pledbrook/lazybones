// Specify SCM excludes
scmExclusions "*.iws", "build/", "*.log", ".lazybones/"

def filterProperties = [:]
filterProperties.group = ask("Define value for 'group': ", null, "group")
filterProperties.version = ask("Define value for 'version' [0.1]: ", "0.1", "version")

processTemplates("build.gradle", filterProperties)
