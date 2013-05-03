package uk.co.cacoethes.util

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 5/2/13
 * Time: 7:29 AM
 * To change this template use File | Settings | File Templates.
 */
class FilterMethodsTest {

    File fileToFilter
    File tmpDir
    File barDir
    File foobaz
    File foobar
    File foo

    @Before
    void setupFileToFilter() {
        fileToFilter = File.createTempFile("foo", null)
        fileToFilter.deleteOnExit()
        fileToFilter.write("hello \${foo}")

        tmpDir = new File("${fileToFilter.path}dir")
        tmpDir.mkdir()
        foo = new File(tmpDir, "foo")
        foo.createNewFile()
        foo.deleteOnExit()
        barDir = new File(tmpDir, "bar")
        barDir.mkdir()
        foobar = new File(barDir, "foobar")
        foobar.createNewFile()
        foobar.deleteOnExit()
        foobaz = new File(barDir, "foobaz")
        foobaz.createNewFile()
        foobaz.deleteOnExit()
    }

    @After
    void deleteTmpDir() {
        foo.delete()
        foobar.delete()
        foobaz.delete()
        barDir.deleteDir()
        tmpDir.deleteDir()
    }

    @Test
    void "basic tests for filtering an individual file"() {
        FilterMethods.filterFile(fileToFilter, "utf-8", [foo: "bar"])
        assert "hello bar" == fileToFilter.text
    }

    @Test(expected = IllegalArgumentException)
    void "illegal argument exception is thrown if the file does not exist"() {
        FilterMethods.filterFile(new File("bar"), "utf-8", [foo: "bar"])
    }

    @Test
    void "test selecting all files"() {
        def files = FilterMethods.getFiles(tmpDir.path, "*/*")
        int count = 0

        files.each{
            count++
            assert "foo" != it.name
        }

        assert 2 == count
    }
}
