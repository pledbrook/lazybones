Lazybones Project Creation Tool
===============================

Lazybones was born out of frustration that [Ratpack](http://ratpack-framework.org/)
does not and will not have a command line tool that will bootstrap a project.
It's a good decision for Ratpack, but I'm lazy and want tools to do the boring
stuff for me.

The tool is very simple: it allows you to create a new project structure for
any framework or library for which the tool has a template. You can even
contribute templates by sending pull requests to this GitHub project or publishing
the packages to the relevant [Bintray repository](https://bintray.com/repo/browse/pledbrook/lazybones-templates)
(more info available below).

[![Build Status](https://drone.io/github.com/pledbrook/lazybones/status.png)](https://drone.io/github.com/pledbrook/lazybones/latest)

Developers
----------

* [Peter Ledbrook](https://github.com/pledbrook)
* [Kyle Boon](https://github.com/kyleboon)
* [Tommy Barker](https://github.com/tbarker9)

Contributors
------------

* [Luke Daley](https://github.com/alkemist)
* [Tomas Lin](https://github.com/tomaslin)


Running it
----------

Grab lazybones from [gvm](http://gvmtool.net):

    gvm install lazybones

or alternatively, grab the distribution [from Bintray](https://bintray.com/pkg/show/general/pledbrook/lazybones-templates/lazybones),
unpack it to a local directory, and then add its 'bin' directory to your `PATH`
environment variable.

To create a new project, run

    lazybones create <template name> <version> <target directory>

So if I wanted to create a skeleton Ratpack project in a new 'my-rat-app'
directory I would run

    lazybones create ratpack-lite 0.1 my-rat-app

I can also create the project in the current directory by passing in '.' as
the target directory.

To see what templates you can install, run

     lazybones list

You can also find out more about a template through the `info` command:

     lazybones info <template name>

This will print a description of the template and what versions are available
for it.

Configuration
-------------

Lazybones will run out of the box without any extra configuration, but you can
control certain aspects of the tool through the configuration file
`~/.lazybones/config.groovy`. This is parsed using Groovy's `ConfigSlurper`, so
if you're familiar with that syntax you'll be right at home. Otherwise, just see
the examples below.

### Custom repositories

Lazybones will by default download the templates from a specific BinTray
repository. If you want to host template packages in a different repository
you can add it to Lazybone's search path via the `bintrayRepositories`
setting:

    bintrayRepositories = [
          "kyleboon/lazybones",
          "pledbrook/lazybones-templates"
    ]

If a template exists in more than one repository, it will be downloaded from the
first repository in the list that it appears in.

### General options

These are miscellaneous options that can be overridden on the command line:

    // <-- This starts a line comment
    // Set logging level - overridden by command line args
    options.logLevel = "SEVERE"

The logging level can either be overridden using the same `logLevel` setting:

    lazybones --logLevel SEVERE info ratpack

or via `--verbose`, `--quiet`, and `--info` options:

    lazybones --verbose info ratpack

The logging level can be one of:

* OFF
* SEVERE
* WARNING
* INFO
* FINE
* FINEST
* ALL

Building it
-----------

This project is split into two parts:

1. The lazybones command line tool; and
2. The project templates.

### The command line tool

The command line tool is created via Gradle's application plugin. The main
class is `uk.co.cacoethes.lazybones.LazyBonesMain`, which currently implements
all the sub-commands (create, list, etc.) as concrete methods.

The main class plus everything else under src/main is packaged into a lazybones
JAR that is included in the distribution zip. The application Gradle plugin
generates a `lazybones` script that then runs the main class with all required
dependencies on the classpath.

To build the distribution, simply run

    ./gradlew distZip

### The project templates

The project templates are simply directory structures with whatever files in
them that you want. Ultimately, the template project directories will be zipped
up and stored on [Bintray](https://bintray.com/repo/browse/pledbrook/lazybones-templates).
From there, lazybones downloads the zips on demand and caches them in a local
user directory (currently ~/.lazybones/templates).

If you want empty directories to form part of the project template, then simply
add an empty .retain file to each one. When the template archive is created,
any .retain files are filtered out (but the containing directories are included).

To package up a template, simply run

    ./gradlew packageTemplate-<templateName>

The name of the project template comes from the containing directory. So the
template directory structure in src/templates/ratpack-lite results in a template
called 'ratpack-lite', which can be packaged with

    ./gradlew packageTemplate-ratpack-lite

The project template archive will be created in the build directory with the
name '<template name>-template-<version>.zip'. See the small section below on
how the template version is derived.

You can also package all the templates in one fell swoop:

    ./gradlew packageTemplates

Once a template is packaged up, you can publish it to a generic (non-Maven)
Bintray repository by running

    ./gradlew publish-<templateName>

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

If you don't want to publish your template you can install it locally using the
installTemplate task.

     ./gradlew installTemplate-<templateName>

This will install the template to ~/.lazybones/templates so that you can use it without
moving it to bintray first.

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

*For a more comprehensive overview, read the [Template Developers Guide](https://github.com/pledbrook/lazybones/wiki/Template-developers-guide)*

If you have an idea for a project template and want to add it to lazybone's
library, then you have two options:

1. Fork this repo, add your template source to src/templates and submit a pull
   request
2. Keep the source in your own repository, build a zip package for the template,
   publish it to Bintray and finally send a link request to the
   pledbrook/lazybones-templates repository

The second option, a binary contribution, is currently the preferred one.
Otherwise the source for this project could grow too large. Plus it's good for
contributors to take responsibility for publishing their own binaries.

Requirements for a project template:

* Must have a VERSION file in the root directory containing just the current
  version number of the template
* A README, README.txt, README.md (or any README.\* file) in the root of the
  project. This file will be displayed straight after a new project is created
  from the template, so it should give some information about what the template
  contains and how to use it
* An optional lazybones.groovy post install script in the root of the template
  directory (see below for more details). It runs right after the template is
  installed and is deleted after successful completion.
* The name of the binary must be of the form &lt;name>-template-&lt;version>.zip and
  should _not_ contain a parent directory. So a README file must be at the top
  level of the zip.
* The name of the template should ideally be of the form &lt;tool/framework>-&lt;variant>,
  where the variant is optional. For example: ratpack-lite, dropwizard,
  grails-cqrs.

The lazybones.groovy post install script is a generic groovy script with a few extra
helper methods:

* `ask(String message, defaultValue = null)` - asks the user a question and returns their answer, or `defaultValue` if no
answer is provided

* `ask(String message, String propertyName, defaultValue = null)` - works similarily to the ask above, but allows
grabbing variables from the command line as well based on the `propertyName`.

* `processTemplates(String filePattern, Map substitutionVariables)` - use ant pattern matching to find files and filter their
contents in place using Groovy's `SimpleTemplateEngine`.

* `hasFeature(String featureName)` - checks if the script has access to a feature, `hasFeature("ask")` or
`hasFeature("fileFilter")` would both return true

Here is a very simple example `lazybones.groovy` script that asks the user for
a couple of values and uses those to populate parameters in the template's build
file:

    def params = [:]
    params["groupId"] = ask("What is the group ID for this project?")
    params["version"] = ask("What is the project's initial version?", "version", "0.1")

    processTemplates("*.gradle", params)
    processTemplates("pom.xml", params)

The main Gradle build file might then look like this:

    apply plugin: "groovy"

    <% if (group) { %>group = "${group}"<% } %>
    version = "${version}"

The `${}` expressions are executed as Groovy expressions and they have access
to any variables in the parameter map passed to `processTemplates()`. Scriptlets,
i.e. code inside `<% %>` delimiters, allow for more complex logic.

