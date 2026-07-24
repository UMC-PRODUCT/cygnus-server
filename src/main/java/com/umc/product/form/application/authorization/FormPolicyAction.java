package com.umc.product.form.application.authorization;

public enum FormPolicyAction {
    FORM_READ("form:read"),
    FORM_CREATE("form:create"),
    FORM_UPDATE("form:update"),
    FORM_PUBLISH("form:publish"),
    FORM_CLOSE("form:close"),
    FORM_DELETE("form:delete"),
    STRUCTURE_MANAGE("form-structure:manage"),
    RESPONSE_CREATE("form-response:create"),
    RESPONSE_READ("form-response:read"),
    RESPONSE_UPDATE("form-response:update"),
    RESPONSE_SUBMIT("form-response:submit"),
    RESPONSE_DELETE("form-response:delete"),
    ANSWER_MANAGE("form-answer:manage");

    private final String id;

    FormPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
