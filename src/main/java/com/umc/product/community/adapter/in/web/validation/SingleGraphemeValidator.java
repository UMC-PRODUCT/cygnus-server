package com.umc.product.community.adapter.in.web.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SingleGraphemeValidator implements ConstraintValidator<SingleGrapheme, String> {

    private static final Pattern GRAPHEME = Pattern.compile("\\X");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || GRAPHEME.matcher(value).matches();
    }
}
