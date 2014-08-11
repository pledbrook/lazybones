package uk.co.cacoethes.lazybones.config

/**
 * Created by pledbrook on 09/08/2014.
 */
class BooleanConverter implements Converter<Boolean> {
    @Override
    Boolean toType(String value) {
        return value ? Boolean.valueOf(value) : null
    }

    @Override
    String toString(Boolean value) {
        return value ? String.valueOf(value) : null
    }

    @Override
    boolean validate(Object value) {
        return value == null || value instanceof Boolean
    }
}
