package com.umc.product.global.graphql.relay;

/**
 * 전역 ID 인코딩에 사용하는 canonical 타입 이름.
 * 도메인 간에 동일 엔티티를 참조할 때 반드시 이 상수를 사용해 표기 불일치를 막는다.
 */
public final class GlobalIdTypes {

    public static final String MEMBER = "Member";
    public static final String CHALLENGER = "Challenger";
    public static final String GISU = "Gisu";
    public static final String CHAPTER = "Chapter";
    public static final String SCHOOL = "School";
    public static final String PROJECT = "Project";
    public static final String PROJECT_MEMBER = "ProjectMember";
    public static final String PROJECT_APPLICATION = "ProjectApplication";
    public static final String MATCHING_ROUND = "MatchingRound";
    public static final String FORM = "Form";
    public static final String FORM_SECTION = "FormSection";
    public static final String FORM_QUESTION = "FormQuestion";
    public static final String FORM_OPTION = "FormOption";
    public static final String FORM_RESPONSE = "FormResponse";
    public static final String FORM_ANSWER = "FormAnswer";
    public static final String FILE = "File";
    public static final String PRIVACY_TERM = "PrivacyTerm";
    public static final String RECRUITING_SEASON = "RecruitingSeason";
    public static final String RECRUITING_ROUND = "RecruitingRound";
    public static final String RECRUITING_APPLICATION = "RecruitingApplication";
    public static final String RECRUITING_APPLICATION_FORM = "RecruitingApplicationForm";
    public static final String RECRUITING_ROUND_EVALUATOR = "RecruitingRoundEvaluator";
    public static final String RECRUITING_INTERVIEW_QUESTION = "RecruitingInterviewQuestion";
    public static final String RECRUITING_INTERVIEW_SCHEDULE = "RecruitingInterviewSchedule";
    public static final String RECRUITING_INTERVIEW_SESSION = "RecruitingInterviewSession";
    public static final String RECRUITING_APPLICATION_EVALUATION = "RecruitingApplicationEvaluation";
    public static final String RECRUITING_DECISION_HISTORY = "RecruitingDecisionHistory";

    private GlobalIdTypes() {
    }
}
