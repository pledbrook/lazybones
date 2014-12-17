package uk.co.cacoethes.util

/**
 * Enumeration representing the various naming conventions, including the two
 * intermediate forms: camel case and lower case hyphenated.
 */
enum NameType {
    CAMEL_CASE,
    PROPERTY(CAMEL_CASE, Naming.&propertyToCamelCase, Naming.&camelCaseToProperty),
    HYPHENATED,
    NATURAL(HYPHENATED, Naming.&naturalToHyphenated, Naming.&hyphenatedToNatural),
    UNKNOWN(getUnknownFunction(), getUnknownFunction())

    private final NameType intermediateType
    private final Closure toIntermediateFn
    private final Closure fromIntermediateFn

    private NameType() {
        this(null, getIdentityFunction(), getIdentityFunction())
    }

    private NameType(Closure toIntermediate, Closure fromIntermediate) {
        this(null, toIntermediate, fromIntermediate)
    }

    private NameType(NameType intermediateType, Closure toIntermediate, Closure fromIntermediate) {
        this.intermediateType = intermediateType ?: this
        this.toIntermediateFn = toIntermediate
        this.fromIntermediateFn = fromIntermediate
    }

    NameType getIntermediateType() { return this.intermediateType }

    /**
     * Uses the assigned function to convert a name string from the current
     * type to its intermediate form.
     */
    String toIntermediate(String s) {
        return this.toIntermediateFn.call(s)
    }

    /**
     * Uses the assigned function to convert a name string from an intermediate
     * form to this type.
     */
    String fromIntermediate(String s) {
        return this.fromIntermediateFn.call(s)
    }

    /**
     * Returns an identity function that simply returns a name unchanged.
     * This must be called as a method, not a property during initialisation
     * of the enum.
     */
    private static Closure getIdentityFunction() {
        return { String s -> return s }
    }

    /**
     * Returns a special function that throws an exception if it's ever called.
     * It should only be used by the UNKNOWN type. This must be called as a
     * method, not a property during initialisation of the enum.
     */
    private static Closure getUnknownFunction() {
        return { String s ->
            throw new UnsupportedOperationException("Unable to convert to or from an unknown name type")
        }
    }
}
