Map filterProperties = [:]
filterProperties.group = ask("Define value for 'group': ", "group")
filterProperties.version = ask("Define value for 'version' [0.1]: ", "version", "0.1")
getString()
processTemplates("build.gradle", filterProperties)

println(5.daysFromNow)
