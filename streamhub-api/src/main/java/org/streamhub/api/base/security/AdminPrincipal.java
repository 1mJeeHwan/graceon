package org.streamhub.api.base.security;

/**
 * Authenticated operator, derived from the access-token claims and set as the
 * Spring Security principal. Carries the church scope so data-access rules can
 * restrict CHURCH_MANAGER operators to their own church without a DB lookup.
 *
 * @param id       admin account id
 * @param role     {@link AuthoritiesConstants#SYSTEM} or {@link AuthoritiesConstants#CHURCH_MANAGER}
 * @param churchId owning church (null for SYSTEM)
 */
public record AdminPrincipal(Long id, String role, Long churchId) {

    public boolean isSystem() {
        return AuthoritiesConstants.SYSTEM.equals(role);
    }

    /**
     * True for roles that read across all churches (SYSTEM and the global read-only VIEWER). Drives
     * read-scope bypass: a VIEWER has no church of its own, so it is treated as unscoped for reads
     * (writes are blocked by permissions, not scope).
     */
    public boolean isUnscoped() {
        return isSystem() || AuthoritiesConstants.VIEWER.equals(role);
    }
}
