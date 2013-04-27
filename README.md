Lazybones Project Creation Tool
===============================

Lazybones was born out of frustration that [Ratpack](http://ratpack-framework.org/)
does not and will not have a command line tool that will bootstrap a project.
It's a good decision for Ratpack, but I'm lazy and want tools to do the boring
stuff for me.

The tool is very simple: it allows you to create a new project structure for
any framework or library for which the tool has a template. You can even
contribute templates sending pull requests to this GitHub project (more info
available below).

Running it
----------

*Note* These instructions will only apply once lazybones becomes a gvm candidate.

Grab lazybones from gvm:

    gvm install lazybones

To create a new project, run

    lazybones create <template name> <version> <target directory>

So if I wanted to create a skeleton Ratpack project in a new 'my-rat-app'
directory I would run

    lazybones create ratpack-lite 0.1 my-rat-app

I can also create the project in the current directory by passing in '.' as
the target directory.

*TODO* Implement

     lazybones list
     lazybones create <template name> <target directory>


Building it
-----------

This project is split into two parts:

1. The lazybones command line tool; and
2. The project templates.

### The command line tool

There's really nothing to this part of the build right now. It's just a Groovy
script in the 'bin' directory.

### The project templates

The project templates are simply directory structures with whatever files in
them that you want. Ultimately, the template project directories will be zipped
up and stored on [BinTray](https://bintray.com/repo/browse/pledbrook/lazybones-templates).
From there, lazybones downloads the zips on demand and caches them in a local
user directory (currently ~/.groovy/lazybones-templates).

If you want empty directories to form part of the project template, then simply
add an empty .retain file to each one. When the template archive is created,
any .retain files are filtered out (but the containing directories are included).

To package up a template, simply run

    ./gradlew packageTemplate#<templateName>

The name of the project template comes from the containing directory. So the
template directory structure in src/templates/ratpack-lite results in a template
called 'ratpack-lite', which can be packaged with

    ./gradlew packageTemplate#ratpack-lite

The project template archive will be created in the build directory with the
name '<template name>-template-<version>.zip'. See the small section below on
how the template version is derived.

You can also package all the templates in one fell swoop:

    ./gradlew packageTemplates

Once a template is packaged up, you can publish it to a generic (non-Maven)
BinTray repository by running

    ./gradlew publish#<templateName>

This will initially fail, because the build does not know where to publish to.
That's quickly fixed by adding a gradle.properties file in the root of this
project that contains at least these properties:

    repo.url=https://api.bintray.com/content/your_bintray_username/lazybones-templates
    repo.username=your_bintray_username
    repo.apiKey=your_bintray_apikey

You can then publish new versions of templates whenever you want. Note that you
cannot _republish_ with this mechanism, so remember to increment the version if
you need to.

Finally, you can publish the whole shebang (unusual) with

    ./gradlew publishAll

And that's it for the project templates.

#### Template versions

You define the version of a template by putting a VERSION file in the root
directory of the template that contains just the version number. For example,
you specify a version of 1.2.8 for the ratpack-lite template by adding the file
src/templates/ratpack-lite/VERSION with the contents

    1.2.8

That's it! The VERSION file will automatically be excluded from the project
template archive.

Contributing templates
----------------------

If you have an idea for a project template and want to add it to lazybone's
library, then you have two options:

1. Fork this repo, add your template source to src/templates and submit a pull
   request
2. Keep the source in your own repository, build a zip package for the template,
   publish it to BinTray and finally send a link request to the
   pledbrook/lazybones-templates repository

The second option, a binary contribution, is currently the preferred one.
Otherwise the source for this project could grow too large. Plus it's good for
contributors to take responsibility for publishing their own binaries.

Requirements for a project template:

* Must have a VERSION file in the root directory containing just the current
  version number of the template
* A README, README.txt, README.md (or any README.* file) in the root of the
  project. This file will be displayed straight after a new project is created
  from the template, so it should give some information about what the template
  contains and how to use it
* The name of the binary must be of the form <name>-template-<version>.zip and
  should _not_ contain a parent directory. So a README file must be at the top
  level of the zip.
* The name of the template should ideally be of the form <tool/framework>-<variant>,
  where the variant is optional. For example: ratpack-lite, dropwizard,
  grails-cqrs.
