package com.umc.product.inhouse.application.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class UmcProductTempPasswordGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^*-_=+";
    private static final String ALL = LETTERS + DIGITS + SYMBOLS;
    private static final int LENGTH = 16;

    public String generate() {
        List<Character> characters = new ArrayList<>(LENGTH);
        characters.add(pick(LETTERS));
        characters.add(pick(DIGITS));
        characters.add(pick(SYMBOLS));
        for (int index = characters.size(); index < LENGTH; index++) {
            characters.add(pick(ALL));
        }
        Collections.shuffle(characters, RANDOM);
        StringBuilder password = new StringBuilder(LENGTH);
        characters.forEach(password::append);
        return password.toString();
    }

    private char pick(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}
