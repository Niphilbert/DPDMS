package com.itzetech.dpdms.user;

public enum UserStatus {
    /** A client who registered and has not yet entered the e-mailed code. */
    PENDING_VERIFICATION,
    /** A staff member the administrator invited; must choose a new password at first sign-in. */
    INVITED,
    ACTIVE,
    DEACTIVATED
}
