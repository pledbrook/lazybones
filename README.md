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
* [Russell Hart](https://github.com/rhart)
* [Dave Syer](https://github.com/dsyer)
* [Andy Duncan](https://github.com/andyjduncan)


Running it
----------

Grab lazybones from [gvm](http://gvmtool.net):

    gvm install lazybones

or alternatively, grab the distribution [from Bintray](https://bintray.com/pkg/show/general/pledbrook/lazybones-templates/lazybones),
unpack it to a local directory, and then add its 'bin' directory to your `PATH`
environment variable.

To create a new project, run

    lazybones create <template name> <template version> <target directory>

So if you wanted to create a skeleton Ratpack project in a new 'my-rat-app'
directory you would run

    lazybones create ratpack-lite 0.1 my-rat-app

Named templates are all stored on Bintray, but you can install them directly
from a URL too:

    lazybones create http://dl.bintray.com/kyleboon/lazybones/java-basic-template-0.1.zip my-app

Of course it can be pretty laborious copying and pasting URLs around, so Lazybones
allows you to configure aliases for URLs. By adding the following configuration to
your Lazybones settings file, `~/.lazybones/config.groovy` (see below for more details
on this), you can install the template by name:

    templates {
        mappings {
            myTmpl = ""
        }
    }

In other words, you could now run

    lazybones create myTmpl my-app

Note that when using the URL option, there is no need to specify a version.

If a mapped template has the same name as a remote template, the mapped
template will be used by `create`; essentially creating a simple override 
mechanism.

There is just one more thing to say about the `create` command: by default it
creates the specified directory and puts the initial project in there. If you
want to unpack a template in the current directory instead, for example if you
have already created the project directory, then just pass '.' as the directory:

    lazybones create ratpack .

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

    ./gradlew packageTemplate<TemplateName>

The name of the project template comes from the containing directory, which is
assumed to be lowercase hyphenated. The template name is the equivalent camel
case form. So the template directory structure in src/templates/ratpack-lite
results in a template called 'RatpackLite', which can be packaged with

    ./gradlew packageTemplateRatpackLite

The project template archive will be created in the build directory with the
name '<template name>-template-<version>.zip'. See the small section below on
how the template version is derived.

You can also package all the templates in one fell swoop:

    ./gradlew packageAllTemplates

Once a template is packaged up, you can publish it to a generic (non-Maven)
Bintray repository by running

    ./gradlew publishTemplate<TemplateName>

This will initially fail, because the build does not know where to publish to.
That's quickly fixed by adding a gradle.properties file in the root of this
project that contains at least these properties:

    repo.username=your_bintray_username
    repo.apiKey=your_bintray_apikey

You can then publish new versions of templates whenever you want. Note that you
cannot _republish_ with this mechanism, so remember to increment the version if
you need to.

Finally, you can publish the whole shebang (unusual) with

    ./gradlew publishAllTemplates

If you don't want to publish your template you can install it locally using the
installTemplate rule.

     ./gradlew installTemplate<TemplateName>

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

Read the [Template Developers Guide](https://github.com/pledbrook/lazybones/wiki/Template-developers-guide)
for information on how to create and publish Lazybones templates.
