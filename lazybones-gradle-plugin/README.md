# Lazybones Gradle plugin

The mechanics of publishing Lazybones templates is straightforward and could
be done manually. That doesn't mean it's not a lot of work though. If you want
to manage and publish Lazybones templates, we strongly recommend you use Gradle
along with this plugin.

The plugin allows you to manage multiple templates, giving you the tools to
package, install, and publish them individually or all together. In addition,
since version 1.1 you can also easily set up subtemplates. Let's see how you
use the plugin.

## Installation

At the current time, the plugin is not in the Gradle plugin portal, so you have
to set it up manually in the `buildscript` section of your build file:

    buildscript {
        repositories {
            maven {
                url "http://dl.bintray.com/pledbrook/plugins"
            }
        }
    
        dependencies {
            classpath "uk.co.cacoethes:lazybones-gradle:1.2.5-SNAPSHOT"
        }
    }

	apply plugin: "uk.co.cacoethes.lazybones-templates"
	...

## Conventions & required configuration

The Lazybones plugin relies on a whole set of conventions so that you can get
started as quickly and painlessly as possible. The basic directory structure
that the plugin relies on looks like this:

    <root>
      ├── build.gradle
      └── templates/
          ├── mytmpl/
          │
          .
          .
          
Each directory under 'templates' contains the source files for one project
template. The name of the project template derives from the name of the
directory. In the above example, we end up with a project template named
'mytmpl'. For more information on what goes inside these project template
directories, see the [Template Developers Guide](https://github.com/pledbrook/lazybones/wiki/Template-developers-guide).

The project structure isn't the whole story. There are three pieces of
information that you must provide:
 
* the name of a Bintray repository to publish the templates to
* the username to use when publishing them
* the API key for that username

You provide that information using a dedicated configuration block in your
build file:

    lazybones {
    	repositoryName = "<user>/<repo>"      // Bintray repository
    	repositoryUsername = "dilbert"
    	repositoryApiKey = "DFHWIRUEFHIWEJFNKWEJBFLWEFEFHLKDFHSLKDF"
    }

(Prior to version 1.1, you had to provide a `repositoryUrl` property in place
of `repositoryName`. This had to be a full URL of the form
https://api.bintray.com/content/\<user\>/\<repo\>)

You should ideally keep the username and API key in a separate gradle.properties
file that doesn't get put into source control. Otherwise you're good to go.

## Tasks

The plugin adds 3 rules and 3 concrete tasks to your project. The 3 rules are:

* `packageTemplate<TmplName>` - Packages the named project template directory
as a zip file.
  
* `installTemplate<TmplName>` - Copies the template package (the zip file) into
your local Lazybones cache.

* `publishTemplate<TmplName>` - Publishes the named template package to Bintray
so that other people can use it.

The template name is derived from the corresponding directory name. The plugin
assumes that the directory name is in lower-case hyphenated form (such as
my-proj-template) and turns that into camel case for the template name (e.g.
MyProjTemplate). So, to package and install one of your project templates, you
just execute

    gradle installTemplateMyProjTemplate
    
Each of these rules has a corresponding task that applies the rule to every
template in your project:

* `packageAllTemplates`
* `installAllTemplates`
* `publishAllTemplates`

As long as you stick to the conventions, that's all you need.
 
## Managing subtemplates

As of version 0.7 of Lazybones, template authors can create subtemplates inside
their project templates. These allow users to perform extra code generation in
a project after it has been created from a Lazybones project template.

From version 1.1 of the Gradle plugin you can easily set up subtemplates. There
are basically two steps:

1. Add the subtemplates as directories alongside the project templates, giving
   each directory a `subtmpl-` prefix to its name.

2. Add a directive to the `lazybones` configuration block telling the plugin
   which subtemplates are to be packaged in which project templates.

The first of these will result in a project structure like this:

    <root>
      ├── build.gradle
      └── templates/
          ├── grails-standard/
          ├── subtmpl-controller/
          ├── subtmpl-domain-class/
          .
          .

The `subtmpl-` prefix ensures that the plugin won't attempt to publish the
subtemplates, since they should not exist independently of a project template.

Once you have created the subtemplate directories and populated them with
files and a post-install script, you need to link them to project templates.
To do that, just add this setting:

    lazybones {
        ...
        template "grails-standard" includes "controller", "domain-class"
	}

This states that the 'grails-standard' project template should include the
'subtmpl-controller' and 'subtmpl-domain-class' subtemplates. Note that you
don't need to include the `subtmpl-` prefix in the configuration setting. It's
implied.

Now when you package the 'grails-standard' project template, it will
automatically include the 'subtmpl-controller' and 'subtmpl-domain-class'
packages too.

## Advanced configuration

Even though the Lazybones Gradle plugin makes use of conventions, you can still
override most of them by setting properties in the `lazybones` configuration
block. Here is a selection of them:

* `templateDirs` - set to a `FileCollection` containing the locations of the
project template directories.

* `packagesDir` - a `File` representing the location where the template package
files are created.

* `installDir` - a `File` representing the location where template packages are
installed to.

* `licenses` - a list of licence names, such as "Apache-2.0". These are required
if you would like the plugin to create the corresponding package in Bintray
during the publish process. Otherwise, you'll have to manually create the
package via Bintray's web UI.

* `publish` - a boolean determining whether packages should automatically be
published in Bintray once they've been uploaded. The default is `false`, which
is the preferred option as it gives you an opportunity to roll back the release.

The full set of options are defined on the [`LazybonesConventions`](https://github.com/pledbrook/lazybones/blob/master/lazybones-gradle-plugin/src/main/groovy/uk/co/cacoethes/gradle/lazybones/LazybonesConventions.groovy)
class.

For more advanced use cases, you can configure the plugin's tasks directly. The
package tasks are instances of the standard Gradle `Zip` task, while the install
tasks are instances of the standard `Copy`.

The package publishing is done through the [`BintrayGenericUpload`](https://github.com/pledbrook/lazybones/blob/master/lazybones-gradle-plugin/src/main/groovy/uk/co/cacoethes/gradle/tasks/BintrayGenericUpload.groovy),
which you can use to defer initialisation of the publish credentials or any other
properties.
