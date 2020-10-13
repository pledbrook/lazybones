package uk.co.cacoethes.gradle.tasks

import groovy.json.JsonBuilder
import org.gradle.api.*
import org.gradle.api.tasks.*

/**
 * Task for uploading artifacts to a generic Bintray repository.
 */
class BintrayGenericUpload extends DefaultTask {
    static final String BASE_BINTRAY_API_URL = "https://api.bintray.com/"

    /** The location on the local filesystem of the artifact to publish. */
    @InputFile
    File artifactFile

    /** The URL path of the published artifact, relative to {@link #repositoryUrl}. */
    @Input
    String artifactUrlPath

    /**
     * The base URL of the Bintray repository to publish to. For example:
     * https://api.bintray.com/content/pledbrook/lazybones-templates
     */
    @Optional
    @Input
    String repositoryUrl

    /**
     * The Bintray repository name to publish to, formed of [user]/[repo]. For
     * example, this could be "pledbrook/lazybones-templates".
     */
    @Optional
    @Input
    String repositoryName

    /** The name of the package that this task will upload. */
    @Optional
    @Input
    String packageName

    /**
     * A list of the licenses that apply to the package. This is only required
     * when creating a new OSS package in Bintray.
     */
    @Optional
    @Input
    List<String> licenses

    /**
     * The URL where the source code for the templates is hosted. This is only
     * required for OSS projects.
     */
    @Optional
    @Input
    String vcsUrl

    /**
     * Determines whether the artifact will be published as soon as it's
     * uploaded. If this is {@code false}, you will need to publish the
     * artifact separately so that users can access it.
     */
    @Input
    boolean publish

    /**
     * The username of the account to publish as. The account must of course
     * have permission to publish to the target repository.
     */
    @Input
    String username

    /** The Bintray API key for the {@link #username} account. */
    @Input
    String apiKey

    @TaskAction
    def publish() {
        if (!packageExists(packageName)) {
            createPackage(packageName, licenses, vcsUrl)
        }
        uploadPackage(calculateUploadUrl())
    }

    protected void uploadPackage(String targetUrl) {
        logger.lifecycle "Streaming artifact ${artifactFile} to Bintray at URL ${targetUrl}"
        createConnection(targetUrl, "PUT").with {
            doOutput = true
            
            def fileInputStream = artifactFile.newInputStream()
            try {
                outputStream << fileInputStream
            }
            catch (Throwable ex) {
                logger.error "Failed to upload to Bintray: " + ex.message
                throw ex
            }
            finally {
                fileInputStream.close()
                outputStream.close()
            }

            def status = responseCode
            if (!(status in 200..<300)) {
                logger.error """\
Failed to upload to Bintray (status ${status}):

    ${errorStream.text}
"""
                throw new GradleException("Unexpected status ${status} from Bintray")
            }
        }
    }

    protected void createPackage(String pkgName, List<String> licenses, String vcsUrl) {
        def conn = createConnection(calculateCreatePackageUrl(), "POST")
        logger.info "Creating package $pkgName via ${conn.URL}"
        conn.with {
            doOutput = true
            doInput = true
            setRequestProperty "Content-Type", "application/json;charset=utf-8"

            outputStream.withWriter("UTF-8") { Writer w ->
                def data = [name: pkgName]
                if (licenses) data.licenses = licenses
                if (vcsUrl) data.'vcs_url' = vcsUrl
                new JsonBuilder(data).writeTo(w)
            }

            if (!(responseCode in 200..<300)) {
                logger.error "Failed to create package ${pkgName}. Response from Bintray:"
                logger.error errorStream.text

                throw new GradleException("Could not create package '$pkgName': $responseMessage")
            }
        }
    }

    protected boolean packageExists(String pkgName) {
        logger.info "Checking if package $pkgName already exists"
        def conn = createConnection(calculatePackageUrl(pkgName), "GET")
        return conn.responseCode != HttpURLConnection.HTTP_NOT_FOUND
    }

    protected HttpURLConnection createConnection(String targetUrl, String httpVerb) {
        HttpURLConnection conn = new URI(targetUrl).toURL().openConnection()
        conn.with {
            // Add basic authentication header.
            setRequestProperty "Authorization", "Basic " + "$username:$apiKey".getBytes().encodeBase64().toString()

            requestMethod = httpVerb
        }

        return conn
    }

    protected String calculateUploadUrl() {
        if (repositoryUrl) {
            return repositoryUrl + (!repositoryUrl.endsWith('/') ? '/' : '') +
                    artifactUrlPath + ';publish=' + (publish ? '1' : '0')
        }
        else {
            return BASE_BINTRAY_API_URL + "content/" + repositoryName +
                    (!repositoryName.endsWith('/') ? '/' : '') + artifactUrlPath +
                    ';publish=' + (publish ? '1' : '0')
        }
    }

    protected String calculateCreatePackageUrl() {
        if (repositoryUrl) {
            return repositoryUrl.replaceFirst("/content/", "/packages/")
        }
        else {
            return BASE_BINTRAY_API_URL + "packages/$repositoryName"
        }
    }

    protected String calculatePackageUrl(String pkgName) {
        if (repositoryUrl) {
            return repositoryUrl.replaceFirst("/content/", "/packages/") + "/$pkgName"
        }
        else {
            return BASE_BINTRAY_API_URL + "packages/$repositoryName/$pkgName"
        }
    }
}
