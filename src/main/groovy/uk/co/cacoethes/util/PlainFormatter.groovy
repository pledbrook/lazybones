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
        def message = record.message + '\n'
        if(record.thrown) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            message += sw.toString()
        }

        return message
    }
}
