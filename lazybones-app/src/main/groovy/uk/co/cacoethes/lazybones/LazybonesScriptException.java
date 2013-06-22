package uk.co.cacoethes.lazybones;

/**
 * Thrown when the Lazybones post-install script for a template throws an error
 * of any sort.
 */
public class LazybonesScriptException extends RuntimeException {
    public LazybonesScriptException() {
        super();
    }

    public LazybonesScriptException(String message) {
        super(message);
    }

    public LazybonesScriptException(String message, Throwable cause) {
        super(message, cause);
    }

    public LazybonesScriptException(Throwable cause) {
        super(cause);
    }
}
