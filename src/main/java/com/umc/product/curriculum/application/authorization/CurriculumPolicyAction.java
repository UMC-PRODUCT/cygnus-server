package com.umc.product.curriculum.application.authorization;

public enum CurriculumPolicyAction {
    ORIGINAL_WORKBOOK_MANAGE("original-workbook:manage"),
    ORIGINAL_WORKBOOK_RELEASE("original-workbook:release"),
    WORKBOOK_SUBMISSION_READ("workbook-submission:read");

    private final String id;

    CurriculumPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
