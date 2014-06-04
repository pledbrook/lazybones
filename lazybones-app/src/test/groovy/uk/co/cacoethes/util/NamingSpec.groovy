package uk.co.cacoethes.util

import static uk.co.cacoethes.util.NameType.*
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by pledbrook on 15/12/2013.
 */
class NamingSpec extends Specification {
    @Unroll
    def "Convert '#name' (#inputType) to #outputType"() {
        expect: "I convert a camel-case name to hyphenated"
        Naming.convert(name, from: inputType, to: outputType) == expected

        where:
        inputType   |  outputType   |  name               |  expected
        CAMEL_CASE  |  HYPHENATED   |  null               |  null
        CAMEL_CASE  |  HYPHENATED   |  ""                 |  ""
        CAMEL_CASE  |  HYPHENATED   |  "john"             |  "john"
        CAMEL_CASE  |  HYPHENATED   |  "John"             |  "john"
        CAMEL_CASE  |  HYPHENATED   |  "JohnDoe"          |  "john-doe"
        CAMEL_CASE  |  HYPHENATED   |  "JohnDoeMD"        |  "john-doe-MD"
        CAMEL_CASE  |  HYPHENATED   |  "JOHNDoe"          |  "JOHN-doe"
        CAMEL_CASE  |  HYPHENATED   |  "johnDOEMd"        |  "john-DOE-md"
        CAMEL_CASE  |  HYPHENATED   |  "JOHNDOE"          |  "JOHNDOE"
        CAMEL_CASE  |  HYPHENATED   |  "ABeeCeeD"         |  "a-bee-cee-d"
        CAMEL_CASE  |  HYPHENATED   |  "AA"               |  "AA"
        CAMEL_CASE  |  HYPHENATED   |  "Sample2Book"      |  "sample-2-book"
        CAMEL_CASE  |  HYPHENATED   |  "Sample245Book"    |  "sample-245-book"
        HYPHENATED  |  CAMEL_CASE   |  "john"             |  "John"
        HYPHENATED  |  CAMEL_CASE   |  "John"             |  "John"
        HYPHENATED  |  CAMEL_CASE   |  "john-doe"         |  "JohnDoe"
        HYPHENATED  |  CAMEL_CASE   |  "john-doe-MD"      |  "JohnDoeMD"
        HYPHENATED  |  CAMEL_CASE   |  "JOHN-doe"         |  "JOHNDoe"
        HYPHENATED  |  CAMEL_CASE   |  "john-DOE-md"      |  "JohnDOEMd"
        HYPHENATED  |  CAMEL_CASE   |  "JOHNDOE"          |  "JOHNDOE"
        HYPHENATED  |  CAMEL_CASE   |  "a-bee-cee-d"      |  "ABeeCeeD"
        HYPHENATED  |  CAMEL_CASE   |  "AA"               |  "AA"
        HYPHENATED  |  CAMEL_CASE   |  "sample2"          |  "Sample2"
        HYPHENATED  |  CAMEL_CASE   |  "sample-2-book"    |  "Sample2Book"
        HYPHENATED  |  CAMEL_CASE   |  "sample-245-book"  |  "Sample245Book"
        PROPERTY    |  CAMEL_CASE   |  "abc"              |  "Abc"
        PROPERTY    |  CAMEL_CASE   |  "johnDoe"          |  "JohnDoe"
        PROPERTY    |  CAMEL_CASE   |  "aBeeCeeD"         |  "ABeeCeeD"
        PROPERTY    |  CAMEL_CASE   |  "ABCDee"           |  "ABCDee"
        PROPERTY    |  CAMEL_CASE   |  "AA"               |  "AA"
        CAMEL_CASE  |  PROPERTY     |  "Abc"              |  "abc"
        CAMEL_CASE  |  PROPERTY     |  "JohnDoe"          |  "johnDoe"
        CAMEL_CASE  |  PROPERTY     |  "ABeeCeeD"         |  "aBeeCeeD"
        CAMEL_CASE  |  PROPERTY     |  "ABCDee"           |  "ABCDee"
        CAMEL_CASE  |  PROPERTY     |  "AA"               |  "AA"
        PROPERTY    |  NATURAL      |  "abc"              |  "Abc"
        PROPERTY    |  NATURAL      |  "johnDoe"          |  "John Doe"
        PROPERTY    |  NATURAL      |  "aBeeCeeD"         |  "A Bee Cee D"
        PROPERTY    |  NATURAL      |  "ABCDee"           |  "ABC Dee"
        PROPERTY    |  NATURAL      |  "AA"               |  "AA"
        NATURAL     |  PROPERTY     |  "Abc"              |  "abc"
        NATURAL     |  PROPERTY     |  "John Doe"         |  "johnDoe"
        NATURAL     |  PROPERTY     |  "A Bee Cee D"      |  "aBeeCeeD"
        NATURAL     |  PROPERTY     |  "ABC Dee"          |  "ABCDee"
        NATURAL     |  PROPERTY     |  "AA"               |  "AA"
        HYPHENATED  |  NATURAL      |  "abc"              |  "Abc"
        HYPHENATED  |  NATURAL      |  "john-doe"         |  "John Doe"
        HYPHENATED  |  NATURAL      |  "a-bee-cee-d"      |  "A Bee Cee D"
        HYPHENATED  |  NATURAL      |  "ABC-dee"          |  "ABC Dee"
        HYPHENATED  |  NATURAL      |  "AA"               |  "AA"
        NATURAL     |  HYPHENATED   |  "Abc"              |  "abc"
        NATURAL     |  HYPHENATED   |  "John Doe"         |  "john-doe"
        NATURAL     |  HYPHENATED   |  "A Bee Cee D"      |  "a-bee-cee-d"
        NATURAL     |  HYPHENATED   |  "ABC Dee"          |  "ABC-dee"
        NATURAL     |  HYPHENATED   |  "AA"               |  "AA"
        CAMEL_CASE  |  CAMEL_CASE   |  "JohnDoeMD"        |  "JohnDoeMD"
        HYPHENATED  |  HYPHENATED   |  "john-doe-MD"      |  "john-doe-MD"
        PROPERTY    |  PROPERTY     |  "johnDoeMD"        |  "johnDoeMD"
        NATURAL     |  NATURAL      |  "John Doe MD"      |  "John Doe MD"
    }
}
