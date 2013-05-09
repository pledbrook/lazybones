package uk.co.cacoethes.util

import java.util.logging.*

/**
 * This is a java.util.logging formatter that simply prints the log messages
 * as-is, without any decoration. It basically turns log statements into
 * simple {@code println()}s. But, you have the advantage of log levels!
 */
@groovy.transform.CompileStatic
class PlainFormatter extends Formatter {
    String format(LogRecord record) {
        return record.message + '\n'
    }
}
