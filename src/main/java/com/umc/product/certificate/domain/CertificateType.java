package com.umc.product.certificate.domain;

public enum CertificateType {
    COMPLETION("CMP", "수료증"),
    MERIT("MRT", "공로증"),
    PROJECT_PARTICIPATION("PRJ", "프로젝트 참가 확인서");

    private final String serialCode;
    private final String displayName;

    CertificateType(String serialCode, String displayName) {
        this.serialCode = serialCode;
        this.displayName = displayName;
    }

    public String serialCode() {
        return serialCode;
    }

    public String displayName() {
        return displayName;
    }
}
