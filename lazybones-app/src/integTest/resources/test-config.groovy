bintray.default.repository = "pledbrook/lazybones-templates"
bintrayRepositories = [bintray.default.repository]

templates {
    mappings {
        customRatpack = "http://dl.dropboxusercontent.com/u/29802534/custom-ratpack.zip"
        doesNotExist = "file:///does/not/exist"
    }
}
