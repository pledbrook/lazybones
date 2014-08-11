package uk.co.cacoethes.lazybones.config;

/**
 * Created by pledbrook on 09/08/2014.
 */
public interface Converter<T> {
    T toType(String value);
    String toString(T value);
    boolean validate(Object value);
}
