package uk.co.cacoethes.lazybones.commands

import groovy.transform.Canonical

/**
 * Represents all the input data for the Lazybones create command, such as
 * template name, version, etc.
 */
@Canonical
class CreateCommandInfo {
    TemplateArg packageArg
    String requestedVersion
    File targetDir
}
