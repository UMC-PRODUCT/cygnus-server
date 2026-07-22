package com.umc.product.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.response.ApiResponse;

import jakarta.servlet.RequestDispatcher;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@DisplayName("global exception 잔여 계약")
class ExceptionResidualTest {

    private final WebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

    @Test
    @DisplayName("ConstraintViolation은 모든 사용자 메시지를 순서대로 결합한다")
    void ConstraintViolation_메시지를_결합한다() {
        ConstraintViolation<?> first = mock(ConstraintViolation.class);
        ConstraintViolation<?> second = mock(ConstraintViolation.class);
        given(first.getMessage()).willReturn("first");
        given(second.getMessage()).willReturn("second");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(first, second));

        ApiResponse<?> body = body(new GlobalExceptionHandler().handleConstraintViolation(exception, webRequest));

        assertThat(body.getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST.getCode());
        assertThat(body.getResult().toString()).contains("first", "second");
    }

    @Test
    @DisplayName("@Valid field 오류는 같은 필드 메시지를 합치고 null 기본 메시지는 빈 값으로 처리한다")
    void MethodArgumentNotValid_필드_오류를_병합한다() throws Exception {
        Object target = new Form();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, "form");
        binding.rejectValue("name", "first", "첫 오류");
        binding.rejectValue("name", "second", "둘째 오류");
        binding.rejectValue("age", "third", null);
        Method method = Fixture.class.getDeclaredMethod("submit", Form.class);
        MethodArgumentNotValidException exception =
            new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);

        ApiResponse<?> body = body(new GlobalExceptionHandler().handleMethodArgumentNotValid(
            exception, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, webRequest));

        @SuppressWarnings("unchecked")
        java.util.Map<Object, Object> errors = (java.util.Map<Object, Object>)body.getResult();
        assertThat(errors)
            .containsEntry("name", "첫 오류, 둘째 오류")
            .containsEntry("age", "");
    }

    @Test
    @DisplayName("읽을 수 없는 body는 원인 유형별 안전한 안내 문구로 축약한다")
    void HttpMessageNotReadable_메시지를_축약한다() {
        assertUnreadableMessage("Cannot deserialize value", "요청 값의 형식이 올바르지 않아요");
        assertUnreadableMessage("Required request body is missing", "요청 내용이 비어 있어요");
        assertUnreadableMessage("JSON parse error", "요청 형식이 올바르지 않아요");
        assertUnreadableMessage("unknown", "요청 형식이 올바르지 않아요");
        assertUnreadableMessage(null, "요청 형식이 올바르지 않아요");
    }

    @Test
    @DisplayName("필수 parameter 누락과 타입 불일치는 actionable BAD_REQUEST로 변환한다")
    void request_parameter_오류를_변환한다() {
        GlobalExceptionHandler sut = new GlobalExceptionHandler();
        var missing = sut.handleMissingServletRequestParameter(
            new MissingServletRequestParameterException("memberId", "Long"),
            HttpHeaders.EMPTY,
            HttpStatus.BAD_REQUEST,
            webRequest);
        var mismatch = sut.handleTypeMismatch(
            new TypeMismatchException("value", Long.class),
            HttpHeaders.EMPTY,
            HttpStatus.BAD_REQUEST,
            webRequest);

        assertThat(body(missing).getResult().toString()).contains("memberId");
        assertThat(body(mismatch).getResult().toString()).contains("형식");
    }

    @Test
    @DisplayName("미처리 예외는 prod에서 내부 메시지를 숨기고 local에서만 원인을 제공한다")
    void 미처리_예외의_profile별_마스킹을_검증한다() {
        GlobalExceptionHandler sut = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(sut, "activeProfile", "prod");
        ApiResponse<?> production = body(sut.handleUnhandledException(new IllegalStateException("secret"), webRequest));
        ReflectionTestUtils.setField(sut, "activeProfile", "local");
        ApiResponse<?> local = body(sut.handleUnhandledException(new IllegalStateException("debug"), webRequest));

        assertThat(production.getResult()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        assertThat(local.getResult()).isEqualTo("debug");
    }

    @Test
    @DisplayName("CustomErrorController는 servlet status와 중첩 root cause를 공통 envelope로 변환한다")
    void fallback_status와_root_cause를_변환한다() {
        CustomErrorController sut = new CustomErrorController();
        for (HttpStatus status : new HttpStatus[]{
            HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED,
            HttpStatus.FORBIDDEN, HttpStatus.INTERNAL_SERVER_ERROR}) {
            MockHttpServletRequest request = request(status.value());
            assertThat(sut.handleError(request).getStatusCode()).isEqualTo(status);
        }
        MockHttpServletRequest unknown = request(599);
        assertThat(sut.handleError(unknown).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        MockHttpServletRequest absent = new MockHttpServletRequest();
        assertThat(sut.handleError(absent).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        NotImplementedException root = new NotImplementedException("custom");
        MockHttpServletRequest nested = request(500);
        nested.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException(new RuntimeException(root)));
        assertThat(sut.handleError(nested).getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    @DisplayName("NotImplementedException과 BusinessException cause 생성자는 code·message·cause를 보존한다")
    void 예외_생성자를_검증한다() {
        RuntimeException cause = new RuntimeException("cause");
        NotImplementedException defaultException = new NotImplementedException();
        BusinessException caused = new BusinessException(Domain.COMMON, CommonErrorCode.BAD_REQUEST, cause) { };

        assertThat(defaultException.getBaseCode()).isEqualTo(CommonErrorCode.NOT_IMPLEMENTED);
        assertThat(new NotImplementedException("custom").getMessage()).contains("custom");
        assertThat(caused.getCause()).isSameAs(cause);
    }

    private void assertUnreadableMessage(String exceptionMessage, String expected) {
        HttpMessageNotReadableException exception =
            new HttpMessageNotReadableException(exceptionMessage, new MockHttpInputMessage(new byte[0]));
        ApiResponse<?> body = body(new GlobalExceptionHandler().handleHttpMessageNotReadable(
            exception, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, webRequest));
        assertThat(body.getResult().toString()).contains(expected);
    }

    private MockHttpServletRequest request(int status) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/test");
        return request;
    }

    private ApiResponse<?> body(org.springframework.http.ResponseEntity<Object> response) {
        return (ApiResponse<?>)response.getBody();
    }

    private record Form(String name, Integer age) {
        private Form() { this(null, null); }
    }

    private static final class Fixture {
        void submit(Form form) { }
    }
}
