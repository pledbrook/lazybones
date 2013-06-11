package uk.co.cacoethes.util

import spock.lang.Specification

import java.util.logging.Level
import java.util.logging.LogRecord

/**
 * Created with IntelliJ IDEA on 6/10/13
 * @author Tommy Barker
 */
class PlainFormatterSpec extends Specification {

    def "format record spec"() {
        given: "a record with an exception"
        def record = new LogRecord(Level.INFO, "logging")
        record.setThrown(new RuntimeException("oops"))

        when: "a record is formatted"
        def formatter = new PlainFormatter()
        def formattedMessage = formatter.format(record)

        then: "message contains message and exception stacktrace"
        formattedMessage.startsWith("logging\njava.lang.RuntimeException: oops")

        when: "a record without an exception is formatted"
        record = new LogRecord(Level.INFO, "logging")
        formattedMessage = formatter.format(record)

        then: "message only contains message"
        "logging\n" == formattedMessage
    }
}
