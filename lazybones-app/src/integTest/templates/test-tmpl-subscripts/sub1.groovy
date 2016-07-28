def subProperties = [:]
subProperties.group = ask("Define value for 'group1': ", null, "group1")
subProperties.version = ask("Define value for 'version1' [0.1]: ", "0.1", "version1")
subProperties.maxThreads = ask("Maximum number of threads to use [$maxThreads]: ", "${maxThreads}", "maxThreads1")
subProperties
