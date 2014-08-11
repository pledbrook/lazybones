bintrayRepositories = ["pledbrook/lazybones-templates"]

templates {
    mappings {
        customRatpack = "http://dl.dropboxusercontent.com/u/29802534/custom-ratpack.zip"
        doesNotExist = "file:///does/not/exist"
    }
}

test.option.override = "Just an option"
