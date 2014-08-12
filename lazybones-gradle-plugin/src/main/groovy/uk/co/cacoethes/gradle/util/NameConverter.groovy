package uk.co.cacoethes.gradle.util

/**
 * Converts names between different forms, such as camel case and hyphenated.
 */
class NameConverter {
    /**
     * Converts a name in camel case (such as HelloWorld) to the hyphenated
     * equivalent (hello-world). The rules are defined in the specification
     * test and reproduced here.
     * <pre>
     * where:
     *   <b>name</b>        | <b>expected</b>
     *   null               | null
     *   ""                 | ""
     *   "john"             | "john"
     *   "John"             | "john"
     *   "JohnDoe"          | "john-doe"
     *   "JohnDoeMD"        | "john-doe-MD"
     *   "JOHNDoe"          | "JOHN-doe"
     *   "johnDOEMd"        | "john-DOE-md"
     *   "JOHNDOE"          | "JOHNDOE"
     *   "ABeeCeeD"         | "a-bee-cee-d"
     *   "Neo4jPlugin"      | "neo4j-plugin"
     *   "1234ever"         | "1234ever"
     *   "1234Ever"         | "1234-ever"
     *   "Do1234Ever"       | "do1234-ever"
     *   "Do1234"           | "do1234"
     * </pre>
     */
    static String camelCaseToHyphenated(String name) {
        if (!name) return name

        def out = new StringBuilder(name.size() + 5)
        def lexer = new BasicLexer(name)

        out << lexer.nextWord()
        String part
        while (part = lexer.nextWord()) {
            out << '-'
            out << part
        }
        return out.toString()
    }

    /**
     * Converts a hyphenated name (such as hello-world) to the camel-case
     * equivalent (HelloWorld). The rules are defined in the specification
     * test and reproduced here.
     * <pre>
     * where:
     *   <b>name</b>        | <b>expected</b>
     *   null               | null
     *   ""                 | ""
     *   "john"             | "john"
     *   "John"             | "john"
     *   "JohnDoe"          | "john-doe"
     *   "JohnDoeMD"        | "john-doe-MD"
     *   "JOHNDoe"          | "JOHN-doe"
     *   "johnDOEMd"        | "john-DOE-md"
     *   "JOHNDOE"          | "JOHNDOE"
     *   "ABeeCeeD"         | "a-bee-cee-d"
     * </pre>
     */
    static String hyphenatedToCamelCase(String name) {
        if (!name) return name

        def out = new StringBuffer()
        def m = name =~ /-([a-zA-Z])/
        while (m) {
            m.appendReplacement out, m.group(1).toUpperCase()
        }
        m.appendTail out

        out.replace(0, 1, name[0].toUpperCase())
        return out.toString()
    }

    private static class BasicLexer {
        static int UPPER = 0
        static int LOWER = 1
        static int OTHER = 2

        private String source
        private int position

        BasicLexer(String source) {
            this.source = source
        }

        String nextWord() {
            int maxPos = source.size()
            if (position == maxPos) return null

            char ch = source[position]
            def state = getType(ch)

            // Start looking at the next characters
            def pos = position + 1
            while (pos < maxPos) {
                // When this character is different to the one before,
                // it is a word boundary unless this is a lower-case
                // letter and the previous one was upper-case.
                def newState = getType(source[pos])
                if (newState != state && (state != UPPER || newState != LOWER)) break

                // Look ahead if both the previous character and the current
                // one are upper case. If, and only if, the next character is
                // lower case, this character is treated as a word boundary.
                if (state == UPPER && newState == UPPER &&
                        (pos + 1) < maxPos && getType(source[pos + 1]) == LOWER) break

                // Go to next character
                state = newState
                pos++
            }

            def word = source.substring(position, pos)
            position = pos

            // Do we need to lower case this word?
            if (ch.isUpperCase() && (word.size() == 1 || (word[1] as char).isLowerCase())) {
                word = word.toLowerCase()
            }

            return word
        }

        private static int getType(ch) {
            ch = ch as char
            return ch.isUpperCase() ? UPPER : (ch.isLowerCase() || ch.isDigit() ? LOWER : OTHER)
        }
    }
}
