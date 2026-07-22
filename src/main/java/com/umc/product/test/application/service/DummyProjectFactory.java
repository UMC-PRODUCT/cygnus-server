package com.umc.product.test.application.service;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.datafaker.Faker;

/**
 * datafaker 를 사용해 시나리오 시딩용 프로젝트 name / description 을 생성한다.
 * <p>
 * Project.name 컬럼은 length 100, description 은 length 200 이라 생성 시 컷오프한다.
 * 모든 더미 값에는 {@code [SEED]} 접두사가 붙어 운영 화면에서 시딩 데이터를 식별할 수 있다.
 */
@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DummyProjectFactory {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final String NAME_PREFIX = "[SEED] ";

    private final Faker faker = new Faker(Locale.KOREAN);

    public String nextName(int sequence) {
        String raw = "%sUMC 프로젝트 #%04d %s".formatted(NAME_PREFIX, sequence, faker.book().title());
        return clip(raw, MAX_NAME_LENGTH);
    }

    public String nextDescription(int sequence) {
        String raw = "시딩 더미 프로젝트 #%04d — %s".formatted(sequence, faker.lorem().paragraph());
        return clip(raw, MAX_DESCRIPTION_LENGTH);
    }

    String clip(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
