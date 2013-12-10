package uk.co.cacoethes.lazybones.commands

import spock.lang.Specification

/**
 * Created by tbarker on 12/9/13.
 */
class PackageLocationBuilderSpec extends Specification {

    /**
     * verify that issue 87 is fixed
     */
    void "default cache directory starts with user home"() {
        when:
        String cachDir = PackageLocationBuilder.DEFAULT_CACHE_PATH

        then:
        cachDir.startsWith(System.getProperty("user.home"))
    }
}
