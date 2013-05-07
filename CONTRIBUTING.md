Contribution Guidelines
=======================

We love to see contributions to the project and have tried to make it easy to
do so, for example by keeping its scope small and the code equally so. If you
wish to contribute code, then please keep to the following guidelines to
ensure consistency within the codebase and that we have happy users.

Philosophy
----------

Our approach to the project is to keep it small and narrowly focused. Expect new
features to be discussed in-depth before being accepted (or rejected). This is
not a framework, but a simple, easy-to-use tool for creating template projects.
It offers useful utility functions to template authors, but nothing more.

Documentation
-------------

If you contribute anything that changes the behaviour of the application,
document it in the README! This includes new features, additional variants
of behaviour and breaking changes.

Make a note of breaking changes in the pull request because they will need
to go into the release notes.

Testing
-------

This project uses [Spock](http://dosc.spockframework.org/) for its tests. Although
any tests are better than none, Spock tests will be looked on more favourably than
other types (such as JUnit). Plus you'll find writing the tests so much nicer!

Unit tests are a nice to have, but it's the integration tests that are critical.
These are run via

    ./gradlew integTest

and launch the lazybones executable as a separate process. The output is captured
so that you can verify the content. The tests reside in the `src/integTest`
directory and extend `AbstractFunctionalSpec`.

They use [Betamax](http://freeside.co/betamax) to intercept and replay web requests
(such as to the Bintray REST API and download URLs).

Formatting
----------

The rules are simple: use the same formatting as the rest of the code. The
following is a list of the styles we are particularly particular about:

* 4 space indent, no tabs
* a space between if/elseif/catch/etc. keywords and the parenthesis
* elseif/else/catch on their own lines

If in doubt, submit the change and mention in the pull request that you're not
sure about a particular style you used. We'd rather have the contribution and
fix any minor problems than not have the contribution at all.

Ultimately, be aware that the maintainers don't have much time to dedicate to
processing pull requests, so the less work they have to do the more likely and
quickly they can merge those pull requests in.

