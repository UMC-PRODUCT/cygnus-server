package com.umc.product.certificate.domain;

public enum CertificateIssuer {
    UNIVERSITY_MAKEUS_CHALLENGE("University MakeUs Challenge"),
    NEORDINARY("Ne(o)rdinary");

    private final String displayName;

    CertificateIssuer(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
