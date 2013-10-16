package uk.co.cacoethes.lazybones.packagesources

import groovy.util.logging.Log
import uk.co.cacoethes.lazybones.NoVersionsFoundException
import uk.co.cacoethes.lazybones.PackageInfo
import wslite.http.HTTPClientException
import wslite.rest.*

/**
 * The default location for Lazybones packaged templates is on Bintray, which
 * also happens to have a REST API. This class uses that API to interrogate
 * the Lazybones template repository for information on what packages are
 * available and to get extra information about them.
 */
@Log
class BintrayPackageSource implements PackageSource {
    static final String TEMPLATE_BASE_URL = "http://dl.bintray.com/v1/content/"
    static final String API_BASE_URL = "https://bintray.com/api/v1"
    static final String PACKAGE_SUFFIX = "-template"

    final String repoName
    def restClient

    BintrayPackageSource(String repositoryName) {
        repoName = repositoryName
        restClient = new RESTClient(API_BASE_URL)

        // For testing with Betamax: set up a proxy if required. groovyws-lite
        // doesn't currently support the http(s).proxyHost and http(s).proxyPort
        // system properties, so we have to manually create the proxy ourselves.
        def proxy = loadSystemProxy(true)
        if (proxy)  {
            restClient.httpClient.proxy = proxy
            restClient.httpClient.sslTrustAllCerts = true
        }
    }

    String getTemplateUrl(String pkgName, String version) {
        def pkgNameWithSuffix = pkgName + PACKAGE_SUFFIX
        return "${TEMPLATE_BASE_URL}/${repoName}/${pkgNameWithSuffix}-${version}.zip"
    }

    List<String> listPackageNames() {
        def response = restClient.get(path: "/repos/${repoName}/packages")

        def pkgNames = response.json.findAll {
            it.name.endsWith(PACKAGE_SUFFIX)
        }.collect {
            it.name - PACKAGE_SUFFIX
        }

        return pkgNames
    }

    /**
     * Fetches package information from Bintray for the given package name or
     * {@code null} if there is no such package. This may also throw instances
     * of {@code wslite.http.HTTPClientException} if there are any problems
     * connecting to the Bintray API.
     * @param pkgName The name of the package for which you want the information.
     * @return The required package info or {@code null} if the repository
     * doesn't host the requested packaged.
     */
    @SuppressWarnings("ReturnNullFromCatchBlock")
    PackageInfo fetchPackageInfo(String pkgName) {
        def pkgNameWithSuffix = pkgName + PACKAGE_SUFFIX

        def response
        try {
            response = restClient.get(path: "/packages/${repoName}/${pkgNameWithSuffix}")
        }
        catch (HTTPClientException ex) {
            if (ex.response?.statusCode != 404) {
                throw ex
            }

            return null
        }


        // The package may have no published versions, so we need to handle the
        // case where `latest_version` is null.
        def data = response.json
        if (!data.'latest_version') {
            throw new NoVersionsFoundException(pkgName)
        }
        
        def pkgInfo = new PackageInfo(this, data.name - PACKAGE_SUFFIX, data.'latest_version')

        pkgInfo.with {
            versions = data.versions as List
            owner = data.owner
            if (data.desc) description = data.desc
            url = data.'desc_url'
        }

        return pkgInfo
    }

    /**
     * Reads the proxy information from the {@code http(s).proxyHost} and {@code http(s).proxyPort}
     * system properties if set and returns a {@code java.net.Proxy} instance configured with
     * those settings. If the {@code proxyHost} setting has no value, then this method returns
     * {@code null}.
     * @param useHttpsProxy {@code true} if you want the HTTPS proxy, otherwise {@code false}.
     */
    private Proxy loadSystemProxy(boolean useHttpsProxy) {
        def propertyPrefix = useHttpsProxy ? "https" : "http"
        def proxyHost = System.getProperty("${propertyPrefix}.proxyHost")
        if (!proxyHost) return null

        def proxyPort = System.getProperty("${propertyPrefix}.proxyPort")?.toInteger()
        proxyPort = proxyPort ?: (useHttpsProxy ? 443 : 80)

        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort))
    }
}
