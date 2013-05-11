package uk.co.cacoethes.lazybones

import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static uk.co.cacoethes.lazybones.LazybonesScript.DEFAULT_ENCODING

/**
 * @author Tommy Barker
 */
class LazybonesScriptTest {

    def script = new LazybonesScript()
    File fileToFilter
    File lazybonesScript

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    void setupFileToFilter() {
        lazybonesScript = testFolder.newFile("lazybones.groovy")
        lazybonesScript.write("""
            def foo = ask("give me foo")
            def bar = ask("give me bar")
            filterFiles('foo', [foo:foo, bar:bar])
        """)
        fileToFilter = testFolder.newFile("foo")
        fileToFilter.write("hello \${foo} and \${bar}")
    }

    @Test(expected = UnsupportedOperationException)
    void "Lazybones script should fail when run is called"() {
        script.run()
    }

    @Test
    void "hasFeature indicates if lazybones has the specified method"() {
        assert script.hasFeature("run")
        assert script.hasFeature("ask")
        assert script.hasFeature("filterFiles")

        assert !script.hasFeature("foobar")
    }

    @Test
    void "default encoding is utf-8"() {
        assert DEFAULT_ENCODING == new LazybonesScript().encoding
    }

    @Test
    void "basic tests for filtering an individual file"() {
        script.filterFileHelper(fileToFilter, [foo: "bar", bar: "bam"])
        assert "hello bar and bam" == fileToFilter.text
    }

    @Test(expected = IllegalArgumentException)
    void "illegal argument exception is thrown if the file does not exist"() {
        script.filterFileHelper(new File("bar"), [foo: "bar"])
    }

    @Test
    void "if value is available in options, then return that"() {
        script.options.foo = "bar"
        assert "bar" == script.ask("give me foo", "foo")
    }

    @Test
    void "if value is returned by user return that"() {
        def originalIn = System.in
        try {
            def newIn = new ByteArrayInputStream("baz".bytes)
            System.in = newIn
            assert "baz" == script.ask("give me foo")
        } finally {
            System.in = originalIn
        }
    }

    @Test
    void "simple base script test"() {
        def originalIn = System.in
        try {
            def newIn = new ByteArrayInputStream("foobar".bytes)
            System.in = newIn
            def scriptText = """
                return ask("give me foo")
            """

            def compiler = new CompilerConfiguration()
            compiler.setScriptBaseClass(LazybonesScript.class.name)
            def shell = new GroovyShell(this.class.classLoader, new Binding(), compiler)
            assert "foobar" == shell.evaluate(scriptText)
        } finally {
            System.in = originalIn
        }
    }

    @Test(expected = IllegalStateException)
    void "filter files throws error if targetDir is not set"() {
        script.filterFiles("*")
    }

    @Test
    void "do full script inheritance with file filtering"() {
        def compiler = new CompilerConfiguration()
        compiler.setScriptBaseClass(LazybonesScript.class.name)
        def shell = new GroovyShell(this.class.classLoader, new Binding(), compiler)

        LazybonesScript script = shell.parse(lazybonesScript) as LazybonesScript
        script.setTargetDir(testFolder.root.path)
        def newLine = System.getProperty("line.separator")
        script.setReader(new ByteArrayInputStream("foobar${newLine}foofam".toString().bytes).newReader())
        script.run()
        assert "hello foobar and foofam" == fileToFilter.text
    }
}
