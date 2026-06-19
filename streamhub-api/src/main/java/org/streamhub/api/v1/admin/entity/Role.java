package org.streamhub.api.v1.admin.entity;

/**
 * Role assigned to an admin account. Drives {@code @PreAuthorize} checks and frontend menu visibility.
 */
public enum Role {
    /** System administrator — full access. */
    SYSTEM,
    /** Church manager — read/write scoped to their own church. */
    CHURCH_MANAGER,
    /** Read-only operator — may view operational pages but cannot mutate. */
    VIEWER
}
