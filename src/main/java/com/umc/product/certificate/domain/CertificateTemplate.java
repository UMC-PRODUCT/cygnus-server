package com.umc.product.certificate.domain;

public enum CertificateTemplate {
    UMC_COURSE_COMPLETION(
        CertificateType.COMPLETION,
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "course",
        "수료증",
        null,
        "certificate/backgrounds/umc-course-completion.pdf"
    ),
    UMC_COURSE_MERIT(
        CertificateType.MERIT,
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "course",
        "공로증",
        "공로증",
        "certificate/backgrounds/umc-course-merit.pdf"
    ),
    UMC_DEMO_DAY_GRAND_PRIZE(
        CertificateType.MERIT,
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "demo",
        "대상",
        "대상",
        "certificate/backgrounds/umc-demo-day-grand-prize.pdf"
    ),
    UMC_DEMO_DAY_FIRST_PRIZE(
        CertificateType.MERIT,
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "demo",
        "최우수상",
        "최우수상",
        "certificate/backgrounds/umc-demo-day-first-prize.pdf"
    ),
    UMC_DEMO_DAY_SECOND_PRIZE(
        CertificateType.MERIT,
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "demo",
        "우수상",
        "우수상",
        "certificate/backgrounds/umc-demo-day-second-prize.pdf"
    ),
    UMC_DEMO_DAY_PARTICIPATION_PRIZE(
        CertificateType.MERIT,
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "demo",
        "장려상",
        "장려상",
        "certificate/backgrounds/umc-demo-day-participation-prize.pdf"
    ),
    UMC_DEMO_DAY_BEST_PART_CHALLENGER(
        CertificateType.MERIT,
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "demo",
        "베스트 파트원",
        "베스트 파트원",
        "certificate/backgrounds/umc-demo-day-best-part-challenger.pdf"
    ),
    UMC_DEMO_DAY_AWS_SPECIAL_PRIZE(
        CertificateType.MERIT,
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "demo",
        "AWS특별상",
        "AWS특별상",
        "certificate/backgrounds/umc-demo-day-aws-special-prize.pdf"
    ),
    NEORDINARY_HACKATHON_GRAND_PRIZE(
        CertificateType.MERIT,
        CertificateIssuer.NEORDINARY,
        "hackathon",
        "대상",
        "대상",
        "certificate/backgrounds/neordinary-hackathon-grand-prize.pdf"
    ),
    NEORDINARY_HACKATHON_FIRST_PRIZE(
        CertificateType.MERIT,
        CertificateIssuer.NEORDINARY,
        "hackathon",
        "최우수상",
        "최우수상",
        "certificate/backgrounds/neordinary-hackathon-first-prize.pdf"
    ),
    NEORDINARY_HACKATHON_SECOND_PRIZE(
        CertificateType.MERIT,
        CertificateIssuer.NEORDINARY,
        "hackathon",
        "우수상",
        "우수상",
        "certificate/backgrounds/neordinary-hackathon-second-prize.pdf"
    ),
    NEORDINARY_HACKATHON_CERTIFICATION_OF_COMPLETION(
        CertificateType.COMPLETION,
        CertificateIssuer.NEORDINARY,
        "hackathon",
        "수료증",
        null,
        "certificate/backgrounds/neordinary-hackathon-certification-of-completion.pdf"
    );

    private static final int DEFAULT_ITEM_COUNT = 4;

    private final CertificateType type;
    private final CertificateIssuer issuer;
    private final String eventKey;
    private final String awardName;
    private final String defaultMeritTitle;
    private final String backgroundResourcePath;

    CertificateTemplate(
        CertificateType type,
        CertificateIssuer issuer,
        String eventKey,
        String awardName,
        String defaultMeritTitle,
        String backgroundResourcePath
    ) {
        this.type = type;
        this.issuer = issuer;
        this.eventKey = eventKey;
        this.awardName = awardName;
        this.defaultMeritTitle = defaultMeritTitle;
        this.backgroundResourcePath = backgroundResourcePath;
    }

    public CertificateType type() {
        return type;
    }

    public CertificateIssuer issuer() {
        return issuer;
    }

    public String eventKey() {
        return eventKey;
    }

    public String awardName() {
        return awardName;
    }

    public String defaultMeritTitle() {
        return defaultMeritTitle;
    }

    public String backgroundResourcePath() {
        return backgroundResourcePath;
    }

    public int itemCount() {
        return DEFAULT_ITEM_COUNT;
    }

    public String brandName() {
        return issuer == CertificateIssuer.NEORDINARY ? "Ne(O)rdinary" : "UMC";
    }
}
