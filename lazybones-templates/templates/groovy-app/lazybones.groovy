def filterProperties = [:]
filterProperties.group = ask("Define value for 'group': ", "group")
filterProperties.version = ask("Define value for 'version' [0.1]: ", "version", "0.1")

processTemplates("build.gradle", filterProperties)

