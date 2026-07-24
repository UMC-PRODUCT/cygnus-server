package com.umc.product.feedback.application.authorization;

public enum FeedbackPolicyAction {
    TEMPLATE_RESOLVE("feedback-template:resolve"),
    RESPONSE_SUBMIT("feedback-response:submit");

    private final String id;

    FeedbackPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
