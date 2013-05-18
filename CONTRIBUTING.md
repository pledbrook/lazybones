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

Commit messages
---------------

It may seem anal to request a particular format for commit messages, but these
are a historical record of what's happening in the code base and consistency
makes investigating that history much easier.

Please follow the advice of the [Phonegap guys](https://github.com/phonegap/phonegap/wiki/Git-Commit-Message-Format)
when crafting commit messages. The advice basically comes down to:

* First line should be maximum 50 characters long
* It should summarise the change and use imperative present tense
* The rest of the commit message should come after a blank line
* We encourage you to use Markdown syntax in the rest of the commit message
* Preferably keep to an 80 character limit on lines in the rest of the message.

If a commit is related to a particular issue, put the issue number after a
hash (#) somewhere in the detail. You can put the issue number in the first
line summary, but only if you can also fit in a useful summary of what was
changed in the commit.

Here's an example git message:

> Make create's version argument optional.
>
> Implements issue #3. If the user doesn't provide an explicit version,
> lazybones retrieves the latest version from Bintray. Integration tests
> added for the `create` command as well.

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

