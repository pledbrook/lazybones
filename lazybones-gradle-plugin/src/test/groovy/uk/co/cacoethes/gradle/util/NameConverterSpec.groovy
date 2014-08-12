package uk.co.cacoethes.gradle.util

import spock.lang.Specification
import spock.lang.Unroll

/**
 *
 */
class NameConverterSpec extends Specification {
    @Unroll
    def "Convert '#name' camel-case to hyphenated"() {
        when: "I convert a camel-case name to hyphenated"
        def convertedName = NameConverter.camelCaseToHyphenated(name)

        then: "The name is converted correctly"
        convertedName == expected

        where:
        name               | expected
        null               | null
        ""                 | ""
        "john"             | "john"
        "John"             | "john"
        "JohnDoe"          | "john-doe"
        "JohnDoeMD"        | "john-doe-MD"
        "JOHNDoe"          | "JOHN-doe"
        "johnDOEMd"        | "john-DOE-md"
        "JOHNDOE"          | "JOHNDOE"
        "ABeeCeeD"         | "a-bee-cee-d"
        "Neo4jPlugin"      | "neo4j-plugin"
        "1234ever"         | "1234ever"
        "1234Ever"         | "1234-ever"
        "Do1234Ever"       | "do1234-ever"
        "Do1234"           | "do1234"
    }

    @Unroll
    def "Convert '#name' to camel-case"() {
        when: "I convert a hyphenated name to camel-case"
        def convertedName = NameConverter.hyphenatedToCamelCase(name)

        then: "The name is converted correctly"
        convertedName == expected

        where:
        name               | expected
        null               | null
        ""                 | ""
        "john"             | "John"
        "John"             | "John"
        "john-doe"         | "JohnDoe"
        "john-doe-MD"      | "JohnDoeMD"
        "JOHN-doe"         | "JOHNDoe"
        "john-DOE-md"      | "JohnDOEMd"
        "JOHNDOE"          | "JOHNDOE"
        "a-bee-cee-d"      | "ABeeCeeD"
        "1234ever"         | "1234ever"
        "1234-ever"        | "1234Ever"
        "do1234-ever"      | "Do1234Ever"
        "do1234"           | "Do1234"
    }
}
