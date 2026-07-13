package com.umc.product.organization.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductMemberActivityPeriodRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductMemberRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductPartMembershipRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UmcProductRequestContractTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .build();

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final DefaultFormattingConversionService conversionService =
        new DefaultFormattingConversionService();

    @Test
    @DisplayName("활동 날짜는 yyyy-MM-dd 형식으로 역직렬화한다")
    void deserializeDateOnlyActivityPeriod() throws Exception {
        // given
        String json = """
            {
              "startDate": "2026-07-13",
              "endDate": "2026-12-31"
            }
            """;

        // when
        CreateUmcProductMemberActivityPeriodRequest request = objectMapper.readValue(
            json, CreateUmcProductMemberActivityPeriodRequest.class
        );

        // then
        assertThat(request.startDate()).isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(request.endDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("활동 날짜에 시각이나 offset이 포함되면 역직렬화를 거부한다")
    void rejectTimestampActivityPeriod() {
        // given
        String json = """
            {
              "startDate": "2026-07-13T00:00:00Z",
              "endDate": null
            }
            """;

        // when & then
        assertThatThrownBy(() -> objectMapper.readValue(json, CreateUmcProductMemberActivityPeriodRequest.class))
            .hasMessageContaining("LocalDate");
    }

    @Test
    @DisplayName("activeOn query parameter는 offset이나 Z가 없는 yyyy-MM-dd 형식만 허용한다")
    void rejectOffsetActiveOnQueryParameter() throws Exception {
        assertStrictActiveOn(
            UmcProductMemberQueryController.class.getDeclaredMethod(
                "search",
                Long.class,
                Long.class,
                com.umc.product.organization.domain.enums.UmcProductPartRole.class,
                com.umc.product.organization.domain.enums.UmcProductLeadershipRole.class,
                com.umc.product.organization.domain.enums.UmcProductPosition.class,
                Long.class,
                LocalDate.class,
                org.springframework.data.domain.Pageable.class
            ),
            6
        );
        assertStrictActiveOn(
            UmcProductSquadQueryController.class.getDeclaredMethod(
                "list", Boolean.class, LocalDate.class
            ),
            1
        );
    }

    @Test
    @DisplayName("멤버 생성에는 한 개 이상의 활동 기간이 필요하다")
    void requireMemberActivityPeriod() {
        // given
        CreateUmcProductMemberRequest request = new CreateUmcProductMemberRequest(
            1L, null, null, java.util.List.of()
        );

        // when
        Set<ConstraintViolation<CreateUmcProductMemberRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
            .contains("activityPeriods");
    }

    @Test
    @DisplayName("멤버 생성 활동 기간 목록은 null 원소를 허용하지 않는다")
    void rejectNullMemberActivityPeriodElement() {
        CreateUmcProductMemberRequest request = new CreateUmcProductMemberRequest(
            1L,
            null,
            null,
            java.util.Collections.singletonList(null)
        );

        Set<ConstraintViolation<CreateUmcProductMemberRequest>> violations = validator.validate(request);

        assertThat(violations).anySatisfy(violation -> {
            assertThat(violation.getPropertyPath().toString()).contains("activityPeriods");
            assertThat(violation.getInvalidValue()).isNull();
        });
    }

    @Test
    @DisplayName("Part 소속 역할로 존재하지 않는 Chapter Lead 입력을 거부한다")
    void rejectChapterLeadRole() {
        // given
        String json = """
            {
              "partId": 1,
              "role": "CHAPTER_LEAD",
              "position": "SERVER_DEVELOPER",
              "startDate": "2026-07-13"
            }
            """;

        // when & then
        assertThatThrownBy(() -> objectMapper.readValue(json, CreateUmcProductPartMembershipRequest.class))
            .hasMessageContaining("CHAPTER_LEAD");
    }

    private void assertStrictActiveOn(Method method, int parameterIndex) {
        TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
        TypeDescriptor targetType = new TypeDescriptor(new MethodParameter(method, parameterIndex));

        assertThat(conversionService.convert("2026-07-13", sourceType, targetType))
            .isEqualTo(LocalDate.of(2026, 7, 13));
        assertThatThrownBy(() -> conversionService.convert("2026-07-13Z", sourceType, targetType))
            .isInstanceOf(ConversionFailedException.class);
        assertThatThrownBy(() -> conversionService.convert("2026-07-13+09:00", sourceType, targetType))
            .isInstanceOf(ConversionFailedException.class);
    }
}
