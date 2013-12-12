import org.apache.commons.io.FilenameUtils

config.file = FilenameUtils.concat(System.getProperty('user.home'), '.lazybones/config.groovy')
cache.dir = FilenameUtils.concat(System.getProperty('user.home'), ".lazybones/templates")
bintray.default.repository = "pledbrook/lazybones-templates"

bintrayRepositories = [bintray.default.repository]
