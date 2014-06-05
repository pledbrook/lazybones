import org.apache.commons.io.FileUtils

def params = [:]
params.pkg = ask("Define value for the package: ", null, "package")
params.cls = ask("Define value for class name: ", null, "class").capitalize()

processTemplates("Entity.groovy", params)

def pkgPath = params.pkg.replace('.' as char, '/' as char)
def filename = params.cls.capitalize() + ".groovy"
def destFile = new File("src/main/groovy/", pkgPath + '/' + filename)
if (targetDir) destFile = new File(targetDir, destFile.path)
destFile.parentFile.mkdirs()

FileUtils.moveFile(new File(templateDir, "Entity.groovy"), destFile)

println "Created new persistence entity ${destFile.path}"
