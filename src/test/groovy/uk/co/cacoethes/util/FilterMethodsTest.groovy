package uk.co.cacoethes.util

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

    @Before
    void setupFileToFilter() {
        fileToFilter = File.createTempFile("foo", null)
        fileToFilter.deleteOnExit()
        fileToFilter.write("hello \${foo}")
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
}
