package uk.co.cacoethes.lazybones.security

import java.security.CodeSource
import java.security.Permission
import java.security.PermissionCollection
import java.security.Permissions
import java.security.Policy
import java.security.ProtectionDomain

/**
 * Based on code from chapter 6 of
 * <a href="http://jaasbook.com/chapters/06/jaas_in_action_chapter0.html">JAAS in Action</a>.
 */
class CompositePolicy extends Policy {
    List policies = Collections.EMPTY_LIST

    CompositePolicy(List policies) { this.policies = new ArrayList(policies) }

    PermissionCollection getPermissions(CodeSource codeSource) {
        return aggregatePermissions { Policy policy -> policy.getPermissions(codeSource) }
    }

    PermissionCollection getPermissions(ProtectionDomain domain) {
        return aggregatePermissions { Policy policy -> policy.getPermissions(domain) }
    }

    boolean implies(ProtectionDomain domain, Permission permission) {
        return policies.any { Policy policy -> policy.implies(domain, permission) }
    }

    void refresh() {
        for (Iterator itr = policies.iterator(); itr.hasNext();) {
            Policy p = (Policy) itr.next()
            p.refresh()
        }
    }

    @SuppressWarnings("NestedForLoop")
    protected PermissionCollection aggregatePermissions(Closure cl) {
        def permissions = new Permissions()
        for (Policy policy in policies) {
            // TODO Does this work in Groovy? Enumerations are awkward IIRC
            for (Permission permission in cl.call(policy)) {
                permissions.add(permission)
            }
        }
        return permissions
    }
}
