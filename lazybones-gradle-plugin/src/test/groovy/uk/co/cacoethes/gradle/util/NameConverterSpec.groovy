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
    }
}
