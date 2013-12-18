# Spring Boot Actuator Sample

You have just created a simple Spring Boot project in Groovy incorporating the
Actuator. This includes everything you need to run the application. In this
case, that's a simple JSON endpoint.

In this project you get:

* A Gradle build file
* An application class, `SampleApplication`, implementing a single JSON endpoint
* A JUnit test case for `SampleApplication`

You can build and run this sample using Gradle (>1.6):

```
$ gradle run
```

If you want to run the application outside of Gradle, then first build the JARs
and then use the `java` command:

```
$ gradle build
$ java -jar build/libs/*.jar
```

Then access the app via a browser (or curl) on http://localhost:8080.
