package uk.co.cacoethes.lazybones.config

/**
 * Created by pledbrook on 09/08/2014.
 */
class IntegerConverter implements Converter<Integer> {
    @Override
    Integer toType(String value) {
        return value ? Integer.valueOf(value) : null
    }

    @Override
    String toString(Integer value) {
        return value ? String.valueOf(value) : null
    }

    @Override
    boolean validate(Object value) {
        return value == null || value instanceof Integer
    }
}
