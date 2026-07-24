package com.umc.product.certificate.application.authorization;

public enum CertificatePolicyAction {
    ISSUE_ADMIN("certificate:issue-admin"),
    REVOKE("certificate:revoke");

    private final String id;

    CertificatePolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
