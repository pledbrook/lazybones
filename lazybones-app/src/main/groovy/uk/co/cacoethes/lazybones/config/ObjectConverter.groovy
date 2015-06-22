package uk.co.cacoethes.lazybones.config

/**
 * Created by pledbrook on 22/06/15.
 */
class ObjectConverter implements Converter<Object> {
    @Override
    Object toType(String value) {
        return value
    }

    @Override
    String toString(Object value) {
        return value?.toString()
    }

    @Override
    boolean validate(Object value) {
        return true
    }
}
