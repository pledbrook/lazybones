package uk.co.cacoethes.gradle.tasks

import org.gradle.api.*
import org.gradle.api.tasks.*

/**
 * Task for uploading artifacts to a generic Bintray repository.
 */
class BintrayGenericUpload extends DefaultTask {
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
    @Input
    String repositoryUrl

    /**
     * The username of the account to publish as. The account must of course
     * have permission to publish to the target repository.
     */
    String username

    /** The Bintray API key for the {@link username} account. */
    String apiKey

    @TaskAction
    def publish() {
        def targetUrl = calculateFullUrl()
        logger.lifecycle "Streaming artifact ${artifactFile} to Bintray at URL ${targetUrl}"
        new URI(targetUrl).toURL().openConnection().with {
            // Add basic authentication header.
            setRequestProperty "Authorization", "Basic " + "$username:$apiKey".getBytes().encodeBase64().toString()

            doOutput = true
            requestMethod = "PUT"

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
            if (status < 200 || status >= 300) {
                logger.error """\
Failed to upload to Bintray (status ${status}):

    ${errorStream.text}
"""
                throw new GradleException("Unexpected status ${status} from Bintray")
            }
        }
    }

    protected String calculateFullUrl() {
        return repositoryUrl + (!repositoryUrl.endsWith('/') ? '/' : '') + artifactUrlPath
    }
}

