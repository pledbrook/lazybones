package uk.co.cacoethes.gradle.tasks

import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Shared
import spock.lang.Specification

/**
 *
 */
class BintrayGenericUploadSpec extends Specification {
    @Shared String baseRepoUrl = "https://api.bintray.com/content/pledbrook/lazybones-templates"
    @Shared String mockPath = "dummy/1.0/dummy-template-1.0.zip"

    def "Package URL is resolved correctly"() {
        given: "A Bintray upload task"
        def mockFile = GroovySpy(File, constructorArgs: ["/var/tmp/path/artifact.zip"]) {
            size() >> 1000
            newInputStream() >> new BufferedInputStream(new ByteArrayInputStream("test".getBytes("UTF-8")))
        }

        def project = ProjectBuilder.builder().build()
        def task = project.task("testUpload", type: BintrayGenericUpload) {
            artifactFile = mockFile
            artifactUrlPath = mockPath
            repositoryUrl = repoUrl
            publish = autoPublish
        }

        when: "I calculate the full URL"
        def targetUrl = task.calculateUploadUrl()

        then: "It includes the path separators in the right place"
        targetUrl == expected

        where:
        repoUrl << [ baseRepoUrl, baseRepoUrl + '/' ]
        autoPublish << [ true, false ]
        expected << [ baseRepoUrl + '/' + mockPath + ';publish=1', baseRepoUrl + '/' + mockPath + ';publish=0' ]
    }
}
