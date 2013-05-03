package uk.co.cacoethes.util

import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 5/2/13
 * Time: 7:25 AM
 * To change this template use File | Settings | File Templates.
 */
class FilterMethods {

    static filterFiles(Iterable<File> files, String encoding, Map properties) {
        files.each {File file ->
            filterFile(file, encoding, properties)
        }
    }

    static filterFile(File file, String encoding, Map properties) {
        if(!file.exists()) {
            throw new IllegalArgumentException("file ${file} does not exist")
        }
        def engine = new SimpleTemplateEngine()
        def reader = file.newReader(encoding)
        def template = engine.createTemplate(reader).make(properties)
        def out = new FileOutputStream(file)
        Writer writer = new OutputStreamWriter(out, encoding)
        template.writeTo(writer)
    }

    static Iterable<File> getFiles(String targetDir, String antPattern) {
        def ant = new AntBuilder()
        def scanner = ant.fileScanner {
            fileset(dir: targetDir) {
                include(name: antPattern)
            }
        } as Iterable<File>
    }
}
