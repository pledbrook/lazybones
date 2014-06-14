import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils

import static org.apache.commons.io.FilenameUtils.concat

def params = [:]
params.pkg = ask("Define value for the package: ", null, "package")
params.cls = ask("Define value for class name: ", null, "class").capitalize()

processTemplates("Controller.groovy", params)

if (tmplQualifiers) {
    println "Found command qualifiers: ${tmplQualifiers}"
}

def pkgPath = params.pkg.replace('.' as char, '/' as char)
def filename = params.cls.capitalize() + "Controller.groovy"
def destFile = new File(projectDir, concat(concat("src/main/groovy", pkgPath), filename))
destFile.parentFile.mkdirs()

FileUtils.moveFile(new File(templateDir, "Controller.groovy"), destFile)

println "Created new controller ${FilenameUtils.normalize(destFile.path)}"
