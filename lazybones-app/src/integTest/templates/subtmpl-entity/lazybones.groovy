import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils

import static org.apache.commons.io.FilenameUtils.concat

def params = [:]
params.pkg = ask("Define value for the package: ", null, "package")
params.cls = ask("Define value for class name: ", null, "class").capitalize()

// Pass in parameters from the project template
params.parentGroup = parentParams.group
params.parentVersion = parentParams.version

processTemplates("Entity.groovy", params)

def pkgPath = params.pkg.replace('.' as char, '/' as char)
def filename = params.cls.capitalize() + ".groovy"
def destFile = new File(projectDir, concat(concat("src/main/groovy", pkgPath), filename))
destFile.parentFile.mkdirs()

FileUtils.moveFile(new File(templateDir, "Entity.groovy"), destFile)

println "Created new persistence entity ${FilenameUtils.normalize(destFile.path)}"
