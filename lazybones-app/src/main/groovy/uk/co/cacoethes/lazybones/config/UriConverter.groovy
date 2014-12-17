package uk.co.cacoethes.lazybones.config

/**
 * We use URL for the type here because JsonBuilder stringifies the
 * type as you would expect. It treats URI as an object, and so stringifies
 * the individual properties.
 */
class UriConverter implements Converter<URL> {
    @Override
    URL toType(String value) {
        return value ? new URL(value) : null
    }

    @Override
    String toString(URL value) {
        return value?.toString()
    }

    @Override
    boolean validate(Object value) {
        return value == null || value
    }
}
