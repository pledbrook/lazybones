package uk.co.cacoethes.lazybones

import joptsimple.OptionParser


class OptionParserBuilder {
    //TODO: Maybe move these to an enum
    static final String VERSION = "version"
    static final String LOG_LEVEL = "logLevel"
    static final String INFO = "info"
    static final String QUIET = "quiet"
    static final String VERBOSE = "verbose"
    static final String V = "v"
    static final String STACKTRACE = "stacktrace"

    static OptionParser makeOptionParser() {
        // These are the global options available for all commands.
        def parser = new OptionParser()
        parser.accepts(STACKTRACE, "Show stack traces when exceptions are thrown.")
        parser.acceptsAll([VERBOSE, V], "Display extra information when running commands.")
        parser.accepts(QUIET, "Show minimal output.")
        parser.accepts(INFO, "Show normal amount of output (default).")
        parser.accepts(LOG_LEVEL, "Set logging level, e.g. OFF, SEVERE, INFO, FINE, etc.").withRequiredArg()
        parser.accepts(VERSION, "Print out the Lazybones finish and then end.")

        // Ensures that only options up to the sub-command ('create, 'list',
        // etc.) are parsed.
        parser.posixlyCorrect(true)
        return parser
    }
}
