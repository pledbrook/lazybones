package uk.co.cacoethes.lazybones

import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Before
import org.junit.Test

import static uk.co.cacoethes.lazybones.LazyBonesScript.*

class LazyBonesScriptTest {

    def script = new LazyBonesScript()

    @Test(expected = UnsupportedOperationException)
    void "LazyBones script should fail when run is called"() {
        new LazyBonesScript().run()
    }

    @Test
    void "hasFeature indicates if lazybones has the specified method"() {
        def script = new LazyBonesScript()

        assert script.hasFeature("run")
        assert script.hasFeature("ask")
        assert script.hasFeature("filterFiles")

        assert !script.hasFeature("foobar")
    }

    @Test
    void "when asking a question, ask returns data in options if already exists"() {
        script.options.foo = "bar"
        assert "bar" == script.ask("foo:", "foo")
    }

    @Test
    void "response is returned if the option does not exist and user provides a response, otherwise the default is returned"() {
        def originalIn = System.in
        def originalOut = System.out

        try {
            String message = "foo:"
            String option = "foo"
            def newOut = new PrintStream(new ByteArrayOutputStream())
            System.out = newOut
            System.in = new ByteArrayInputStream("bar".bytes)
            assert "bar" == script.ask(message, option)
            System.in = new ByteArrayInputStream("".bytes)
            assert "bam" == script.ask(message, option, "bam")
        } finally {
            System.in = originalIn
            System.out = originalOut
        }
    }

    @Test
    void "test using LazyBonesScript as a base script"() {
        def originalIn = System.in
        def originalOut = System.out

        try {
            def newOut = new PrintStream(new ByteArrayOutputStream())
            System.out = newOut
            System.in = new ByteArrayInputStream("bar".bytes)
            def scriptText = """
                return ask('give me foo')

            """
            def compiler = new CompilerConfiguration()
            compiler.setScriptBaseClass(LazyBonesScript.class.getName())
            def shell = new GroovyShell(this.class.classLoader, new Binding(), compiler)
            assert "bar" == shell.evaluate(scriptText)
        } finally {
            System.in = originalIn
            System.out = originalOut
        }
    }

    @Test
    void "default encoding is utf-8"() {
        assert DEFAULT_ENCODING == new LazyBonesScript().encoding
    }
}
