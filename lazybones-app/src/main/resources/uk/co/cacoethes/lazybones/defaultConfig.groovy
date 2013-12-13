config.file = new File(System.getProperty('user.home'), '.lazybones/config.groovy').path
cache.dir = new File(System.getProperty('user.home'), ".lazybones/templates").path
bintray.default.repository = "pledbrook/lazybones-templates"

bintrayRepositories = [bintray.default.repository]
