package com.umc.product.community.adapter.in.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CodePointLengthValidator implements ConstraintValidator<CodePointLength, String> {

    private int min;
    private int max;
    private boolean trim;

    @Override
    public void initialize(CodePointLength annotation) {
        min = annotation.min();
        max = annotation.max();
        trim = annotation.trim();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String candidate = trim ? value.strip() : value;
        int length = candidate.codePointCount(0, candidate.length());
        return length >= min && length <= max;
    }
}
