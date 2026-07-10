package com.umc.product.certificate.domain;

public enum CertificateTemplate {
    UMC_COURSE_COMPLETION(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "CMP",
        true,
        true,
        "course",
        "수료증",
        null,
        "certificate/backgrounds/umc-course-completion.pdf"
    ),
    UMC_COURSE_MERIT(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "course",
        "공로증",
        "공로증",
        "certificate/backgrounds/umc-course-completion.pdf"
    ),
    UMC_DEMO_DAY_GRAND_PRIZE(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "demo",
        "대상",
        "대상",
        "certificate/backgrounds/umc-demo-day-grand-prize.pdf"
    ),
    UMC_DEMO_DAY_FIRST_PRIZE(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "demo",
        "최우수상",
        "최우수상",
        "certificate/backgrounds/umc-demo-day-first-prize.pdf"
    ),
    UMC_DEMO_DAY_SECOND_PRIZE(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "demo",
        "우수상",
        "우수상",
        "certificate/backgrounds/umc-demo-day-second-prize.pdf"
    ),
    UMC_DEMO_DAY_PARTICIPATION_PRIZE(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "demo",
        "장려상",
        "장려상",
        "certificate/backgrounds/umc-demo-day-participation-prize.pdf"
    ),
    UMC_DEMO_DAY_AWS_SPECIAL_PRIZE(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "demo",
        "AWS특별상",
        "AWS특별상",
        "certificate/backgrounds/umc-demo-day-aws-special-prize.pdf"
    ),
    UMC_DEMO_DAY_BEST_CHALLENGER(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "demo",
        "베스트 챌린저",
        "베스트 챌린저",
        "certificate/backgrounds/umc-demo-day-best-part-challenger.pdf"
    ),
    UMC_HACKATHON_CERTIFICATION_OF_COMPLETION(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "CMP",
        true,
        false,
        "hackathon",
        "수료증",
        null,
        "certificate/backgrounds/umc-course-completion.pdf"
    ),
    UMC_HACKATHON_GRAND_PRIZE(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "hackathon",
        "대상",
        "대상",
        "certificate/backgrounds/umc-demo-day-grand-prize.pdf"
    ),
    UMC_HACKATHON_FIRST_PRIZE(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "hackathon",
        "최우수상",
        "최우수상",
        "certificate/backgrounds/umc-demo-day-first-prize.pdf"
    ),
    UMC_HACKATHON_SECOND_PRIZE(
        CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
        "MRT",
        false,
        false,
        "hackathon",
        "우수상",
        "우수상",
        "certificate/backgrounds/umc-demo-day-second-prize.pdf"
    ),
    NEORDINARY_HACKATHON_GRAND_PRIZE(
        CertificateIssuer.NEORDINARY,
        "MRT",
        false,
        false,
        "hackathon",
        "대상",
        "대상",
        "certificate/backgrounds/neordinary-hackathon-grand-prize.pdf"
    ),
    NEORDINARY_HACKATHON_FIRST_PRIZE(
        CertificateIssuer.NEORDINARY,
        "MRT",
        false,
        false,
        "hackathon",
        "최우수상",
        "최우수상",
        "certificate/backgrounds/neordinary-hackathon-first-prize.pdf"
    ),
    NEORDINARY_HACKATHON_SECOND_PRIZE(
        CertificateIssuer.NEORDINARY,
        "MRT",
        false,
        false,
        "hackathon",
        "우수상",
        "우수상",
        "certificate/backgrounds/neordinary-hackathon-second-prize.pdf"
    ),
    NEORDINARY_HACKATHON_CERTIFICATION_OF_COMPLETION(
        CertificateIssuer.NEORDINARY,
        "CMP",
        true,
        false,
        "hackathon",
        "수료증",
        null,
        "certificate/backgrounds/neordinary-hackathon-certification-of-completion.pdf"
    );

    private static final int DEFAULT_ITEM_COUNT = 4;

    private final CertificateIssuer issuer;
    private final String serialCode;
    private final boolean graduationRequired;
    private final boolean selfIssuable;
    private final String eventKey;
    private final String awardName;
    private final String defaultMeritTitle;
    private final String backgroundResourcePath;

    CertificateTemplate(
        CertificateIssuer issuer,
        String serialCode,
        boolean graduationRequired,
        boolean selfIssuable,
        String eventKey,
        String awardName,
        String defaultMeritTitle,
        String backgroundResourcePath
    ) {
        this.issuer = issuer;
        this.serialCode = serialCode;
        this.graduationRequired = graduationRequired;
        this.selfIssuable = selfIssuable;
        this.eventKey = eventKey;
        this.awardName = awardName;
        this.defaultMeritTitle = defaultMeritTitle;
        this.backgroundResourcePath = backgroundResourcePath;
    }

    public CertificateIssuer issuer() {
        return issuer;
    }

    public String serialCode() {
        return serialCode;
    }

    public boolean requiresGraduation() {
        return graduationRequired;
    }

    public boolean supportsSelfIssue() {
        return selfIssuable;
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

    public String englishCertificateTitle() {
        if (graduationRequired || "공로증".equals(awardName)) {
            return "Certificate of Completion";
        }
        return "Certificate of Award";
    }

    public String englishTitleLine1(Long generation) {
        return generation + ordinalSuffix(generation) + " " + brandName();
    }

    public String englishTitleLine2() {
        if ("공로증".equals(awardName)) {
            return "APPRECIATION";
        }
        if ("course".equals(eventKey)) {
            return "COMPLETION";
        }
        if ("hackathon".equals(eventKey)) {
            return "HACKATHON";
        }
        return "DEMO DAY";
    }

    public String koreanTitle(String displayAwardName) {
        if ("hackathon".equals(eventKey)) {
            return "해커톤 " + displayAwardName;
        }
        if ("demo".equals(eventKey) && usesDemoTitlePrefix(displayAwardName)) {
            return "데모데이 " + displayAwardName;
        }
        return displayAwardName;
    }

    public String koreanSubtitle(Long generation, String displayAwardName) {
        return englishTitleLine1(generation) + " " + koreanTitle(displayAwardName);
    }

    public String defaultDescription(Long generation, String displayAwardName) {
        String generationKo = generation + "기";
        String line1 = englishTitleLine1(generation);
        if ("course".equals(eventKey) && graduationRequired) {
            return "위 챌린저는 전국 대학생 IT 연합 동아리 University MakeUs Challenge " + generationKo
                + " 과정을 성실히 수료하였기에 이 증서를 수여합니다.";
        }
        if ("course".equals(eventKey) && "공로증".equals(awardName)) {
            return "위 운영진은 전국 대학생 IT 연합 동아리 University MakeUs Challenge " + generationKo
                + " 과정의 발전에 기여하였기에 이 증서를 수여합니다.";
        }
        if ("demo".equals(eventKey) && isBestAward()) {
            return "위 챌린저는 전국 대학생 IT 연합 동아리 University MakeUs Challenge " + generationKo
                + " 과정에서 최고의 역량과 성과를 보였기에 이 증서를 수여합니다.";
        }
        if ("demo".equals(eventKey) && "AWS특별상".equals(awardName)) {
            return "위 챌린저는 " + line1
                + " DEMO DAY에서 AWS 기술 활용의 우수성을 인정받아 이 증서를 수여합니다.";
        }
        if ("hackathon".equals(eventKey) && graduationRequired) {
            return "위 챌린저는 " + line1 + " HACKATHON을 성실히 수료하였기에 이 증서를 수여합니다.";
        }
        return "위 챌린저는 " + line1 + " " + englishTitleLine2()
            + "에서 이와 같이 우수한 성적을 거두었기에 이 증서를 수여합니다.";
    }

    private boolean usesDemoTitlePrefix(String displayAwardName) {
        return "대상".equals(displayAwardName)
            || "최우수상".equals(displayAwardName)
            || "우수상".equals(displayAwardName)
            || "장려상".equals(displayAwardName);
    }

    private boolean isBestAward() {
        return "베스트 챌린저".equals(awardName);
    }

    private String ordinalSuffix(Long value) {
        long number = value == null ? 0L : Math.abs(value);
        long lastTwoDigits = number % 100;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 13) {
            return "th";
        }
        return switch ((int) (number % 10)) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
