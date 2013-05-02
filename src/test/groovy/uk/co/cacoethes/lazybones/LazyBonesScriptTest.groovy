package uk.co.cacoethes.lazybones

import org.junit.Test

class LazyBonesScriptTest {

    @Test(expected = UnsupportedOperationException)
    void "LazyBones script should fail when run is called"() {
        new LazyBonesScript().run()
    }

    @Test
    void "hasFeature indicates if lazybones has the specified method"() {
        def script = new LazyBonesScript()

        assert script.hasFeature("run")
        assert script.hasFeature("ask")
        assert script.hasFeature("filterFiles")

        assert !script.hasFeature("foobar")
    }
}
