package uk.co.cacoethes.util

import groovy.transform.Immutable

/**
 * <p>Provides static methods for converting between different forms of names,
 * such as camel case, hyphenated lower case, and natural. You can find the
 * rules for the conversion in the {@linkplain uk.co.cacoethes.gradle.util.NameConverterSpec
 * unit specification} for this class.</p>
 * <p><em>Note</em> Names that don't conform to the expected formats may lead to
 * unexpected behaviour. Basically the conversions are undefined. Also, sequences
 * of numbers are treated as words, so Sample245Book becomes sample-245-book.</p>
 * <p>The conversion itself always goes through intermediate forms: either
 * camel case, lower case hyphenated, or both. This reduces the amount of code
 * needed to handle multiple types.</p>
 * @author Peter Ledbrook
 */
class Naming {

    /**
     * Converts a string from one naming convention to another.
     * @param args A set of named arguments. Both {@code from} and {@code to}
     * are required and should be the corresponding {@link NameType}
     * @param content The string to convert. If this is either {@code null} or
     * an empty string, then the method returns that value.
     * @return A new string representing {@code content} in the requested
     * form.
     */
    static String convert(Map args, String content) {
        if (!(args.from instanceof NameType)) {
            throw new IllegalArgumentException("Invalid or no value for 'from' named argument: ${args.from}")
        }

        if (!(args.to instanceof NameType)) {
            throw new IllegalArgumentException("Invalid or no value for 'to' named argument: ${args.to}")
        }

        NameWithType name = new NameWithType(NameType.UNKNOWN, content)
        return name.from(args.from).to(args.to)
    }

    /**
     * Starts a conversion of a string from one naming convention to another.
     * It's used like this:
     * <pre>
     * convert("test-string").from(NameType.CAMEL_CASE).to(NameType.HYPHENATED)
     * </pre>
     * Both the {@code from()} and the {@code to()} are required for the
     * conversion to actuall take place.
     * @param content The string to convert. If this is either {@code null} or
     * an empty string, then that value is what is ultimately returned.
     * @return An object that you can call {@code from()} on to specify the
     * current form of the given name.
     */
    static NameWithType convert(String content) {
        return new NameWithType(NameType.UNKNOWN, content)
    }

    /**
     * Converts a name in camel case (such as HelloWorld) to the hyphenated
     * equivalent (hello-world).
     */
    @SuppressWarnings('DuplicateStringLiteral')
    protected static String camelCaseToHyphenated(String name) {
        if (!name) return name

        def out = new StringBuilder(name.size() + 5)
        def lexer = new BasicLexer(name)

        out << lexer.nextWord()
        for (String part = lexer.nextWord(); part; part = lexer.nextWord()) {
            out << '-'
            out << part
        }
        return out.toString()
    }

    /**
     * Converts a hyphenated name (such as hello-world) to the camel-case
     * equivalent (HelloWorld).
     */
    protected static String hyphenatedToCamelCase(String name) {
        if (!name) return name

        def out = new StringBuffer()
        def m = name =~ /-([a-zA-Z0-9])/
        while (m) {
            m.appendReplacement out, m.group(1).toUpperCase()
        }
        m.appendTail out

        out.replace(0, 1, name[0].toUpperCase())
        return out.toString()
    }

    /**
     * Converts a name in hyphenated form to its natural form. Hyphenated is
     * the intermediate form for natural.
     */
    @SuppressWarnings('DuplicateStringLiteral')
    protected static String hyphenatedToNatural(String content) {
        return content.split('-').collect {
            def str = it[0].toUpperCase()
            if (it.size() > 1) str += it[1..-1]
            return str
        }.join(' ')
    }

    /**
     * Converts a name in natural form into its corresponding intermediate
     * form, hyphenated.
     */
    @SuppressWarnings('DuplicateStringLiteral')
    @SuppressWarnings('UnnecessaryElseStatement')
    protected static String naturalToHyphenated(String content) {
        return content.split(' ').collect {
            if (it.size() > 1 && Character.isUpperCase(it[1] as char)) return it
            else return it.toLowerCase()
        }.join('-')
    }

    /**
     * Converts a name in property form into its corresponding intermediate
     * form, camel case.
     */
    @SuppressWarnings('DuplicateStringLiteral')
    protected static String propertyToCamelCase(String content) {
        return content[0].toUpperCase() + (content.size() > 1 ? content[1..-1] : '')
    }

    /**
     * Converts a name in camel case form into its property form. Camel case is
     * the intermediate form for property names.
     */
    @SuppressWarnings('DuplicateStringLiteral')
    @SuppressWarnings('UnnecessaryElseStatement')
    protected static String camelCaseToProperty(String content) {
        def upperBound = Math.min(content.size(), 3)
        if (content[0..<upperBound].every { Character.isUpperCase(it as char) }) {
            return content
        }
        else {
            return content[0].toLowerCase() + (content.size() > 1 ? content[1..-1] : '')
        }
    }

    /**
     * Stores a name string along with the current form of that name. There is
     * no verification that the given name string actually conforms to the given
     * type.
     */
    @Immutable
    private static class NameWithType {
        NameType type
        String content

        /**
         * Effectively assigns a name type to the current name string and
         * converts that string to its corresponding intermediate type. There
         * is no verification that name string is actually of the given form.
         * @param type The form to convert from.
         * @return A new {@code NameType} object with the same name string
         * as this one, but with the assigned type.
         */
        @SuppressWarnings('UnnecessaryElseStatement')
        NameWithType from(NameType type) {
            return new NameWithType(type.intermediateType, type.toIntermediate(content))
        }

        /**
         * Performs the conversion from an intermediate type to the target
         * type. If the current type isn't one of the intermediate types, this
         * method will fail to work properly. It won't throw any exceptions
         * though, the result will just be incorrect.
         * @param type The name type to convert the current name string to.
         * @return The converted name string.
         */
        String to(NameType type) {
            // If the from and to types have different intermediate types, we
            // first need to convert between the two intermediate types.
            def currentContent = this.content
            if (this.type.intermediateType == NameType.CAMEL_CASE && type.intermediateType == NameType.HYPHENATED) {
                currentContent = camelCaseToHyphenated(currentContent)
            }
            else if (this.type.intermediateType == NameType.HYPHENATED &&
                    type.intermediateType == NameType.CAMEL_CASE) {
                currentContent = hyphenatedToCamelCase(currentContent)
            }

            return type.fromIntermediate(currentContent)
        }
    }

    private static class BasicLexer {
        static final int UPPER = 0
        static final int LOWER = 1
        static final int OTHER = 2

        private final String source
        private int position

        BasicLexer(String source) {
            this.source = source
        }

        String nextWord() {
            int maxPos = source.size()
            if (position == maxPos) return null

            char ch = source[position]
            def state = getType(ch as char)

            // Start looking at the next characters
            def pos = position + 1
            while (pos < maxPos) {
                // When this character is different to the one before,
                // it is a word boundary unless this is a lower-case
                // letter and the previous one was upper-case.
                def newState = getType(source[pos] as char)
                if (newState != state && (state != UPPER || newState != LOWER)) break

                // Look ahead if both the previous character and the current
                // one are upper case. If, and only if, the next character is
                // lower case, this character is treated as a word boundary.
                if (state == UPPER && newState == UPPER &&
                        (pos + 1) < maxPos && getType(source[pos + 1] as char) == LOWER) break

                // Go to next character
                state = newState
                pos++
            }

            def word = source[position..<pos]
            position = pos

            // Do we need to lower case this word?
            if (ch.isUpperCase() && (word.size() == 1 || (word[1] as char).isLowerCase())) {
                word = word.toLowerCase()
            }

            return word
        }

        private static int getType(char ch) {
            return ch.isUpperCase() ? UPPER : (ch.isLowerCase() ? LOWER : OTHER)
        }
    }
}
