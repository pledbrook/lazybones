package uk.co.cacoethes.lazybones.commands

import groovy.transform.Canonical
import uk.co.cacoethes.lazybones.PackageInfo

@Canonical
class CreateCommandInfo {
    String packageName
    String requestedVersion
    File targetDir
    PackageInfo packageInfo
}
