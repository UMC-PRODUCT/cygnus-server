package com.umc.product.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;

import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.response.code.BaseCode;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;

@DisplayName("GraphQlExceptionAdvice")
class GraphQlExceptionAdviceTest {

    private final GraphQlExceptionAdvice sut = new GraphQlExceptionAdvice();
    private final DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class, RETURNS_DEEP_STUBS);

    @Test
    @DisplayName("접근 거부는 FORBIDDEN error type과 공통 확장 필드로 변환한다")
    void 접근_거부를_변환한다() {
        GraphQLError error = sut.handleAccessDeniedException(new AccessDeniedException("denied"), environment);

        assertError(error, ErrorType.FORBIDDEN, CommonErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("BusinessException의 HTTP status를 GraphQL error type으로 정확히 매핑한다")
    void BusinessException_status를_매핑한다() {
        assertError(sut.handleBusinessException(exception(CommonErrorCode.BAD_REQUEST), environment),
            ErrorType.BAD_REQUEST, CommonErrorCode.BAD_REQUEST);
        assertError(sut.handleBusinessException(exception(CommonErrorCode.UNAUTHORIZED), environment),
            ErrorType.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED);
        assertError(sut.handleBusinessException(exception(CommonErrorCode.FORBIDDEN), environment),
            ErrorType.FORBIDDEN, CommonErrorCode.FORBIDDEN);
        assertError(sut.handleBusinessException(exception(CommonErrorCode.NOT_FOUND), environment),
            ErrorType.NOT_FOUND, CommonErrorCode.NOT_FOUND);
        assertError(sut.handleBusinessException(exception(CommonErrorCode.TOO_MANY_REQUESTS), environment),
            ErrorType.INTERNAL_ERROR, CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("입력 예외와 미처리 예외는 각각 BAD_REQUEST와 마스킹된 INTERNAL_ERROR로 변환한다")
    void 입력과_미처리_예외를_변환한다() {
        GraphQLError badRequest = sut.handleBadRequestException(
            new DateTimeParseException("invalid", "value", 0), environment);
        GraphQLError internal = sut.handleUnhandledException(new IllegalStateException("secret"), environment);

        assertError(badRequest, ErrorType.BAD_REQUEST, CommonErrorCode.BAD_REQUEST);
        assertError(internal, ErrorType.INTERNAL_ERROR, CommonErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(internal.getMessage()).doesNotContain("secret");
    }

    private BusinessException exception(BaseCode code) {
        return new BusinessException(Domain.COMMON, code, "custom") { };
    }

    private void assertError(GraphQLError error, ErrorType type, BaseCode code) {
        assertThat(error.getErrorType()).isEqualTo(type);
        assertThat(error.getExtensions())
            .containsEntry("code", code.getCode())
            .containsEntry("httpStatus", code.getHttpStatus().value());
    }
}
