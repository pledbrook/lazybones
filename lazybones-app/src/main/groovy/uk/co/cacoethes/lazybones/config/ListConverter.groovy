package uk.co.cacoethes.lazybones.config

/**
 * Created by pledbrook on 09/08/2014.
 */
class ListConverter implements Converter<List> {
    private final Class componentType

    ListConverter(Class componentType) {
        this.componentType = componentType
    }

    @Override
    List toType(String value) {
        def converter = Converters.getConverter(componentType)
        return value?.split(/,\s+/)?.collect { converter.toType(it) }
    }

    @Override
    String toString(List value) {
        return value?.join(", ")
    }

    @Override
    boolean validate(Object value) {
        def converter = Converters.getConverter(componentType)
        return value == null || (value instanceof List && value.every { converter.validate(it) })
    }
}
