package uk.co.cacoethes.lazybones.impl

import spock.lang.Specification
import uk.co.cacoethes.lazybones.PackageNotFoundException
import wslite.http.HTTPClientException
import wslite.http.HTTPRequest
import wslite.http.HTTPResponse

class BintrayPackageSourceSpec extends Specification {

    def "Should return the package source's name"() {
        given: "An initialised package source for Bintray"
        final expected = "harley quinn"
        final source = new BintrayPackageSource(expected)

        expect: "The source name to be returned"
        source.name == expected
    }

    def "Should return the number of packages stored in the Bintray repo"() {
        given: "A mock REST client"
        final mockClient = GroovyMock(Map) {
            getHttpClient() >> [:]
        }

        and: "An initialised package source for Bintray"
        final repoName = "pledbrook/lazybones-templates"
        final source = new BintrayPackageSource(repoName, mockClient)

        when:
        final count = source.packageCount

        then:
        count == 10
        mockClient.get(_) >> { Map args -> assert args.path.contains(repoName); [json: [package_count: "10"]] }
    }

    def "Should throw an exception when getTemplateUrl() is called for an unknown package name"() {
        given: "A mock REST client"
        final repoName = "pledbrook/lazybones-templates"
        final mockClient = GroovyMock(Map) {
            getHttpClient() >> [:]
            get(_ as Map) >> { Map args ->
                assert args.path.contains(repoName)
                throw new HTTPClientException("Not found", new HTTPRequest(), new HTTPResponse(statusCode: 404))
            }
        }

        and: "An initialised package source for Bintray"
        final source = new BintrayPackageSource(repoName, mockClient)

        when:
        source.getTemplateUrl("unknown", "1.1")

        then:
        PackageNotFoundException ex = thrown()

    }

    def "Should throw an exception when getPackage() is called for an unknown package name"() {
        given: "A mock REST client"
        final repoName = "pledbrook/lazybones-templates"
        final mockClient = GroovyMock(Map) {
            getHttpClient() >> [:]
            get(_ as Map) >> { Map args ->
                assert args.path.contains(repoName)
                throw new HTTPClientException("Not found", new HTTPRequest(), new HTTPResponse(statusCode: 404))
            }
        }

        and: "An initialised package source for Bintray"
        final source = new BintrayPackageSource(repoName, mockClient)

        when:
        source.getPackage("unknown")

        then:
        PackageNotFoundException ex = thrown()

    }
}
