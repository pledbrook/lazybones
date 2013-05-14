def filterProperties = [:]
filterProperties.group = ask("Define value for 'group': ")
filterProperties.version = ask("Define value for 'version' [0.1]: ", "0.1")

filterFiles("build.gradle", filterProperties)

