package com.umc.product.community.adapter.in.web.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PositiveDecimalIdValidator implements ConstraintValidator<PositiveDecimalId, String> {

    private static final Pattern POSITIVE_DECIMAL = Pattern.compile("[1-9][0-9]*");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (!POSITIVE_DECIMAL.matcher(value).matches()) {
            return false;
        }
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
