package uk.co.cacoethes.lazybones

import groovy.text.SimpleTemplateEngine

import java.lang.reflect.Method

/**
 * Base script that will be applied to th lazybones.groovy root script in a lazybones template
 *
 * @author Tommy Barker
 */
class LazyBonesScript extends Script {

    protected static final String DEFAULT_ENCODING = "utf-8"
    final Map options = [:]

    String targetDir
    String encoding = DEFAULT_ENCODING

    String getEncoding() {
        if(encoding) {
            return encoding
        }
        encoding = DEFAULT_ENCODING
    }

    /**
     * prints a message asking for a property value.  If options already contains the value, that
     * value is returned and the question is not asked.  If the user has no response the default
     * value will be returned.  null can be returned
     *
     * @param message
     * @param optionName
     * @param defaultValue
     * @return
     */
    def ask(String message, String optionName = null, defaultValue = null) {
        if (optionName) {
            if(options.containsKey(optionName)) {
                return options[optionName]
            }
        }

        String line
        System.out.print message
        System.in.withReader {Reader reader ->
            line = reader.readLine()
        }

        if(line) return line

        return defaultValue
    }

    def filterFiles(String filePattern, Map substitutionVariables) {
        if(!targetDir) {
            throw new IllegalStateException("targetDir has not been set")
        }
        def ant = new AntBuilder()
        def scanner = ant.fileScanner {
            fileset(dir: targetDir) {
                include(name: filePattern)
            }
        } as Iterable<File>
        filterFilesHelper(scanner, getEncoding(), substitutionVariables)
        return this
    }

    /**
     * uses the options / cli variables to filter files.  filePattern is usd by the ant <code>fileScanner</code>.
     * See {@link http://groovy.codehaus.org/Using+Ant+from+Groovy} or more details.
     *
     * @param filePattern
     */
    def filterFiles(String filePattern) {
        filterFiles(filePattern, options)
    }

    private void filterFilesHelper(Iterable<File> files, Map properties) {
        files.each {File file ->
            filterFile(file, getEncoding(), properties)
        }
    }

    private void filterFileHelper(File file, Map properties) {
        if(!file.exists()) {
            throw new IllegalArgumentException("file ${file} does not exist")
        }
        def engine = new SimpleTemplateEngine()
        def reader = file.newReader(getEncoding())
        def template = engine.createTemplate(reader).make(properties)
        def out = new FileOutputStream(file)
        Writer writer = new OutputStreamWriter(out, getEncoding())
        template.writeTo(writer)
    }

    @Override
    Object run() {
        throw new UnsupportedOperationException("${this.getClass().name} is meant to be used directly, instead it shouldbe used as a base script")
    }

    boolean hasFeature(String featureName) {
        return this.getClass().methods.any {Method method -> method.name == featureName }
    }
}
