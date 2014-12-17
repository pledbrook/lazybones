package uk.co.cacoethes.lazybones.config

/**
 * Created by pledbrook on 09/08/2014.
 */
class StringConverter implements Converter<CharSequence> {
    @Override
    String toType(String value) {
        return value?.toString()
    }

    @Override
    String toString(CharSequence value) {
        return value
    }

    @Override
    boolean validate(Object value) {
        return value == null || value instanceof CharSequence
    }
}
