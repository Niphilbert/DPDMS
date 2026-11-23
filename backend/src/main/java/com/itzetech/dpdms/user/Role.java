package com.itzetech.dpdms.user;

public enum Role {
    ADMIN("Administrator"),
    PROJECT_MANAGER("Project manager"),
    DEVELOPER("Developer"),
    CLIENT("Client");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isStaff() {
        return this != CLIENT;
    }
}
