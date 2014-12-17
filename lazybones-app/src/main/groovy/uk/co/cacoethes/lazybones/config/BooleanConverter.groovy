package uk.co.cacoethes.lazybones.config

/**
 * Created by pledbrook on 09/08/2014.
 */
class BooleanConverter implements Converter<Boolean> {
    @Override
    @SuppressWarnings("BooleanMethodReturnsNull")
    Boolean toType(String value) {
        return value != null ? Boolean.valueOf(value) : null
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
