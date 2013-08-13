package uk.co.cacoethes.lazybones.security

import java.security.CodeSource
import java.security.PermissionCollection
import java.security.Policy

/**
 * Created by pledbrook on 13/08/2013.
 */
class RuntimePolicy extends Policy {
    @Override
    PermissionCollection getPermissions(CodeSource codeSource) {

    }
}
