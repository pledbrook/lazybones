package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import io.netty.handler.codec.http.HttpRequest
import org.junit.Rule
import org.littleshoot.proxy.ChainedProxy
import org.littleshoot.proxy.ChainedProxyAdapter
import org.littleshoot.proxy.ChainedProxyManager
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.ProxyAuthenticator
import org.littleshoot.proxy.impl.DefaultHttpProxyServer

class ProxyFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()
    HttpProxyServer proxy

    void setup() {
        proxy = DefaultHttpProxyServer.bootstrap().
                withPort(0).
                withProxyAuthenticator(new ProxyAuthenticator() {
                    @Override
                    boolean authenticate(String userName, String password) {
                        return userName == "dummy" && password == "password"
                    }
                }).
                withChainProxyManager(new ChainedProxyManager() {
                    @Override
                    void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies) {
                        chainedProxies.add(new ChainedProxyAdapter() {
                            @Override
                            InetSocketAddress getChainedProxyAddress() {
                                return new InetSocketAddress(recorder.proxyHost, recorder.proxyPort)
                            }
                        })
                    }
                }).start()
        initProxy(proxy.listenAddress)

        filesToDelete << new File(cacheDirPath, "ratpack-0.1.zip")
    }

    void cleanup() {
        proxy.stop()
    }

    @Betamax(tape="create-tape")
    def "Create command works with correct proxy credentials"() {
        given: "Correct proxy credentials"
        env["JAVA_OPTS"] += " -Dlazybones.systemProp.http.proxyUser=dummy"
        env["JAVA_OPTS"] += " -Dlazybones.systemProp.http.proxyPassword=password"

        when: "I run lazybones with the create command for the ratpack template"
        def exitCode = runCommand(["create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "It unpacks the template, retaining file permissions"
        exitCode == 0
        def appDir = new File(baseWorkDir, "ratapp")
        appDir.exists()
        new File(appDir, "gradlew").canExecute()
        new File(appDir, "src/main/groovy").isDirectory()
        new File(appDir, "src/ratpack/public/index.html").isFile()

        and: "It says that the given version of the package is being installed in the target directory"
        output =~ /Creating project from template ratpack 0.1 in 'ratapp'/
    }

    @Betamax(tape="create-tape")
    def "Create command triggers 407 if invalid proxy credentials provided"() {
        given: "Incorrect proxy credentials"
        env["JAVA_OPTS"] += " -Dlazybones.systemProp.http.proxyUser=dilbert"
        env["JAVA_OPTS"] += " -Dlazybones.systemProp.http.proxyPassword=password"

        when: "I run lazybones with the create command for the ratpack template"
        println "Env $env"
        println "Props ${System.properties}"
        def exitCode = runCommand(["create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "The command reports a 407 error"
        exitCode != 0
        output =~ /Unexpected failure: 407 Proxy Authentication Required/
    }

    @Betamax(tape="create-tape")
    def "Create command triggers 407 if no proxy credentials provided"() {
        when: "I run lazybones with the create command for the ratpack template"
        println "Env $env"
        println "Props ${System.properties}"
        def exitCode = runCommand(["create", "ratpack", "0.1", "ratapp"], baseWorkDir)

        then: "The command reports a 407 error"
        exitCode != 0
        output =~ /Unexpected failure: 407 Proxy Authentication Required/
    }
}
