package uk.co.cacoethes.lazybones.commands

import wslite.http.HTTPClientException

import java.util.logging.Level
import java.util.logging.Logger

/**
 * A collection of static methods to ensure consistency between all the commands
 * when Lazybones is run offline.
 */
class OfflineMode {
    static boolean isOffline(ex) {
        return ex instanceof HTTPClientException ? isOffline(ex.cause) :
                [ConnectException, UnknownHostException].any { it.isAssignableFrom(ex.class) }
    }

    @SuppressWarnings("ParameterReassignment")
    static void printlnOfflineMessage(Throwable ex, Logger log, boolean stacktrace) {
        if (ex instanceof HTTPClientException) ex = ex.cause

        println "(Offline mode - run with -v or --stacktrace to find out why)"
        log.fine "(Error message: ${ex.class.simpleName} - ${ex.message})"
        if (stacktrace) log.log Level.WARNING, "", ex
        println()
    }
}
