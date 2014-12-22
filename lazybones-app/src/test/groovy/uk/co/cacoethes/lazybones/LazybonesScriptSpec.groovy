package uk.co.cacoethes.lazybones

import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine
import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static uk.co.cacoethes.lazybones.LazybonesScript.DEFAULT_ENCODING

/**
 * @author Tommy Barker
 */
class LazybonesScriptSpec extends Specification {
    static final String NEW_LINE = System.getProperty("line.separator")

    def script = new LazybonesScript()
    File fileToFilter
    File lazybonesScript

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    void setup() {
        lazybonesScript = testFolder.newFile("lazybones.groovy")
        lazybonesScript.write("""
            def foo = ask("give me foo")
            def bar = ask("give me bar")
            processTemplates('foo', [foo:foo, bar:bar])
        """)
        fileToFilter = testFolder.newFile("foo")
        fileToFilter.write("hello \${foo} and \${bar}")
    }

    void "Lazybones script should fail when run is called"() {
        when:
        script.run()
        then:
        thrown(UnsupportedOperationException)
    }

    void "hasFeature indicates if lazybones has the specified method"() {
        expect:
        script.hasFeature(a)
        !script.hasFeature(b)

        where:
        a << ["run", "ask", "processTemplates"]
        b << ["foobar", "foobaz", "foofam"]
    }

    void "default encoding is utf-8"() {
        expect:
        a == new LazybonesScript().fileEncoding

        where:
        a << [DEFAULT_ENCODING]
    }

    void "basic tests for filtering an individual file"() {
        when:
        script.processTemplateWithEngine(fileToFilter, [foo: "bar", bar: "bam"], new SimpleTemplateEngine(), true)

        then:
        "hello bar and bam" == fileToFilter.text
    }

    void "illegal argument exception is thrown if the file does not exist"() {
        when:
        script.processTemplateWithEngine(new File("bar"), [foo: "bar"], new SimpleTemplateEngine(), true)

        then:
        thrown(IllegalArgumentException)
    }

    void "if value is returned by user return that"() {
        given:
        script = new LazybonesScript()
        script.setReader(createReader("baz"))

        when:
        def response = script.ask("give me foo")

        then:
        "baz" == response
    }

    void "simple base script test"() {

        given:
        def scriptText = """
                return ask("give me foo")
        """
        LazybonesScript script = createScript(scriptText)
        script.setReader(createReader("foobar"))

        when:
        def response = script.run()

        then:
        "foobar" == response
    }

    void "filter files throws error if targetDir is not set"() {
        when:
        script.processTemplates("*", [:])

        then:
        thrown(IllegalStateException)
    }

    void "do full script inheritance with file filtering"() {
        given:
        LazybonesScript script = createScript(lazybonesScript.text)
        script.setProjectDir(testFolder.root)
        script.setTemplateDir(testFolder.root)
        script.setReader(createReader("foobar${NEW_LINE}foofam"))

        when:
        script.run()

        then:
        "hello foobar and foofam" == fileToFilter.text
    }

    void "if the binding already contains the property it is used instead of asking the question"() {
        given:
        def scriptText = """
            return ask("give me foo", null, "foo")
        """
        LazybonesScript script = createScript(scriptText)
        script.binding.foo = "bar"

        when:
        def response = script.run()

        then:
        "bar" == response
        script.parentParams["foo"] == "bar"
    }

    void "ask saves the value for named variables"() {
        given:
        def scriptText = """
            ask("give me foo", null, "foo")
            ask("how old are you?", "-1", "age")
        """
        LazybonesScript script = createScript(scriptText)
        script.setReader(createReader("foobar${NEW_LINE}42${NEW_LINE}"))

        when:
        script.run()

        then:
        script.parentParams["foo"] == "foobar"
        script.parentParams["age"] == "42"
    }

    void "default template engine is SimpleTemplateEngine"() {
        expect:
        SimpleTemplateEngine == new LazybonesScript().templateEngine.class
    }

    LazybonesScript createScript(String text) {
        def compiler = new CompilerConfiguration()
        compiler.setScriptBaseClass(LazybonesScript.class.name)
        def shell = new GroovyShell(this.class.classLoader, new Binding(), compiler)
        shell.parse(text) as LazybonesScript
    }

    Reader createReader(String text) {
        new ByteArrayInputStream(text.bytes).newReader()
    }
}
