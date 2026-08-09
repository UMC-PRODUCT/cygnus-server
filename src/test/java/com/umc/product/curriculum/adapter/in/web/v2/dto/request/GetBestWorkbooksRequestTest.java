package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class GetBestWorkbooksRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    @DisplayName("베스트 워크북 조회 크기는 100까지 허용한다")
    void size100_allowed() {
        assertThat(validator.validate(request(100))).isEmpty();
    }

    @Test
    @DisplayName("베스트 워크북 조회 크기가 100을 초과하면 검증에 실패한다")
    void size101_rejected() {
        assertThat(validator.validate(request(101))).isNotEmpty();
    }

    @Test
    @DisplayName("기수를 지정하지 않으면 전체 기수 베스트 워크북을 조회할 수 있다")
    void gisuId_optional() {
        GetBestWorkbooksRequest request =
            new GetBestWorkbooksRequest(null, null, null, null, null, 0, 20);

        assertThat(validator.validate(request)).isEmpty();
    }

    private GetBestWorkbooksRequest request(int size) {
        return new GetBestWorkbooksRequest(9L, null, null, null, null, null, size);
    }
}
