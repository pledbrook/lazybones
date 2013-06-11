Gradle Groovy project template
------------------------------

You have just created a basic Groovy application. It provides a standard
project structure, and Spock for writing tests

    <proj>
      |
      +- src
          |
          |
          +- main
          |   |
          |   +- groovy
                   |
                   +- // App classes in here!
          |
          +- test
              |
              +- groovy
                   |
                   +- // Spock tests in here!

That's it! You can build / test the application with gradle

    ./gradlew <test | groovyCompile | etc>


