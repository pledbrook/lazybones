import org.apache.commons.io.FileUtils

def params = [:]
params.pkg = ask("Define value for the package: ", null, "package")
params.cls = ask("Define value for class name: ", null, "class").capitalize()

processTemplates("Controller.groovy", params)

def pkgPath = params.pkg.replace('.' as char, '/' as char)
def filename = params.cls.capitalize() + "Controller.groovy"
def destFile = new File("src/main/groovy/", pkgPath + '/' + filename)
if (targetDir) destFile = new File(targetDir, destFile.path)
destFile.parentFile.mkdirs()

FileUtils.moveFile(new File(templateDir, "Controller.groovy"), destFile)

println "Created new controller ${destFile.path}"
