package com.umc.product.global.exception;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.StaleObjectStateException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.umc.product.authentication.domain.exception.EmailVerificationThrottledException;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.exception.DraftSchemaMismatchException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.response.ApiErrorResponseFactory;
import com.umc.product.global.response.ApiResponse;
import com.umc.product.global.response.code.BaseCode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * Spring Security 권한 거부 예외 처리 - 메서드 레벨 보안 및 MVC 내부 인가 실패
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException e, WebRequest request) {
        log.warn("[ACCESS DENIED] {}", e.getMessage());

        return buildResponse(e, CommonErrorCode.FORBIDDEN, HttpHeaders.EMPTY, request, null);
    }

    /**
     * Validation 제약 조건 위반 예외 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
        ConstraintViolationException e,
        WebRequest request
    ) {
        String messages = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));

        log.warn("[CONSTRAINT VIOLATION] {}", messages);

        return buildResponse(e, CommonErrorCode.BAD_REQUEST, HttpHeaders.EMPTY, request, messages);
    }

    /**
     * @Valid 검증 실패 예외 처리
     */
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException e,
        HttpHeaders headers, HttpStatusCode status,
        WebRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors()
            .forEach(fieldError -> {
                String fieldName = fieldError.getField();
                String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
                errors.merge(fieldName, errorMessage,
                    (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", "
                        + newErrorMessage);
            });

        log.warn("[VALIDATION ERROR] {}", errors);

        return buildResponse(e, CommonErrorCode.BAD_REQUEST, HttpHeaders.EMPTY, request, errors);
    }

    /**
     * JSON 파싱 에러 처리 (Request Body 읽기 실패)
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {

        log.warn("JSON 파싱 에러: {}", e.getMessage());

        String errorMessage = e.getMessage();
        String simplifiedMessage = "요청 형식이 올바르지 않아요. 입력한 값을 확인해주세요.";

        if (errorMessage != null) {
            if (errorMessage.contains("Cannot deserialize")) {
                simplifiedMessage = "요청 값의 형식이 올바르지 않아요. 입력한 값을 확인해주세요.";
            } else if (errorMessage.contains("Required request body is missing")) {
                simplifiedMessage = "요청 내용이 비어 있어요. 입력한 값을 확인해주세요.";
            } else if (errorMessage.contains("JSON parse error")) {
                simplifiedMessage = "요청 형식이 올바르지 않아요. 입력한 값을 확인해주세요.";
            }
        }

        return buildResponse(e, CommonErrorCode.BAD_REQUEST, headers, request, simplifiedMessage);
    }

    /**
     * 필수 Request Parameter 누락 예외 처리
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
        MissingServletRequestParameterException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        String detail = String.format("필수 요청 값 '%s'가 없어요. 입력한 값을 확인해주세요.", e.getParameterName());
        log.warn("[MISSING REQUEST PARAMETER] {}", detail);

        return buildResponse(e, CommonErrorCode.BAD_REQUEST, headers, request, detail);
    }

    /**
     * Request Parameter 타입 변환 실패 예외 처리
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
        TypeMismatchException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        log.warn("[REQUEST PARAMETER TYPE MISMATCH] {}", e.getMessage());

        return buildResponse(
            e,
            CommonErrorCode.BAD_REQUEST,
            headers,
            request,
            "요청 값의 형식이 올바르지 않아요. 입력한 값을 확인해주세요."
        );
    }


    /**
     * 처리되지 않은 모든 예외의 기본 핸들러 프로덕션 환경에서는 상세 에러 메시지를 숨김
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnhandledException(Exception e, WebRequest request) {
        log.error("[UNHANDLED EXCEPTION] {}", e.getMessage(), e);

        String errorDetail = isProductionProfile()
            ? CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
            : e.getMessage();

        return buildResponse(e, CommonErrorCode.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, request, errorDetail);
    }

    /**
     * draft 제출 시 스키마 재검증 실패 처리 — 응답 body 에 staleQuestionIds 목록을 함께 노출한다.
     */
    @ExceptionHandler(DraftSchemaMismatchException.class)
    public ResponseEntity<Object> onDraftSchemaMismatch(DraftSchemaMismatchException e, WebRequest request) {
        log.warn("[BUSINESS EXCEPTION] domain={}, code={}, staleQuestionIds={}",
            e.getDomain(), e.getBaseCode().getCode(), e.getStaleQuestionIds());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("staleQuestionIds", e.getStaleQuestionIds());
        ApiResponse<Object> body = ApiErrorResponseFactory.from(e.getBaseCode(), detail);
        return super.handleExceptionInternal(
            e,
            body,
            HttpHeaders.EMPTY,
            e.getBaseCode().getHttpStatus(),
            request
        );
    }

    /**
     * FormResponse 낙관적 락(@Version) CAS 실패를 409 로 매핑한다.
     * <p>
     * Spring 이 감싼 {@link ObjectOptimisticLockingFailureException} 을 먼저 처리하고,
     * 일부 코드 경로(EntityManager 직접 사용 등)에서 원본 Hibernate {@link StaleObjectStateException}
     * 이 그대로 올라오는 케이스도 함께 방어한다. 대상 엔티티가 FormResponse 가 아닐 경우
     * 재던져서 기본 핸들러가 처리하도록 위임한다.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> onOptimisticLockingFailure(
        ObjectOptimisticLockingFailureException e,
        WebRequest request
    ) {
        if (!isFormResponseConflict(e.getPersistentClassName())) {
            return handleUnhandledException(e, request);
        }
        log.warn("[FORM RESPONSE CONCURRENT MODIFICATION] persistentClass={}, id={}",
            e.getPersistentClassName(), e.getIdentifier());
        return buildResponse(e, FormErrorCode.FORM_RESPONSE_CONCURRENT_MODIFICATION, HttpHeaders.EMPTY, request, null);
    }

    @ExceptionHandler(StaleObjectStateException.class)
    public ResponseEntity<Object> onStaleObjectState(StaleObjectStateException e, WebRequest request) {
        if (!isFormResponseConflict(e.getEntityName())) {
            return handleUnhandledException(e, request);
        }
        log.warn("[FORM RESPONSE CONCURRENT MODIFICATION] entityName={}, id={}",
            e.getEntityName(), e.getIdentifier());
        return buildResponse(e, FormErrorCode.FORM_RESPONSE_CONCURRENT_MODIFICATION, HttpHeaders.EMPTY, request, null);
    }

    private boolean isFormResponseConflict(String className) {
        return className != null
            && (className.equals(FormResponse.class.getName()) || className.equals(FormResponse.class.getSimpleName()));
    }

    @ExceptionHandler(value = BusinessException.class)
    public ResponseEntity<Object> onThrowException(BusinessException e, WebRequest request) {
        log.warn("[BUSINESS EXCEPTION] domain={}, code={}, message={}", e.getDomain(), e.getBaseCode().getCode(),
            e.getMessage(), e);

        ApiResponse<Object> body = BusinessExceptionResponseResolver.toApiResponse(e);
        HttpHeaders headers = HttpHeaders.EMPTY;
        if (e instanceof EmailVerificationThrottledException throttled) {
            headers = new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER, Long.toString(throttled.getRetryAfterSeconds()));
        }
        return super.handleExceptionInternal(
            e,
            body,
            headers,
            e.getBaseCode().getHttpStatus(),
            request
        );
    }

    /**
     * 통합 에러 응답 빌더
     */
    private ResponseEntity<Object> buildResponse(
        Exception e, BaseCode code,
        HttpHeaders headers, WebRequest request, Object detail
    ) {
        ApiResponse<Object> body = ApiErrorResponseFactory.from(code, detail);
        return super.handleExceptionInternal(e, body, headers, code.getHttpStatus(), request);
    }

    private ResponseEntity<Object> buildResponse(
        Exception e, BaseCode code,
        HttpHeaders headers, WebRequest request, Object detail,
        String message
    ) {
        ApiResponse<Object> body = ApiErrorResponseFactory.from(code, message, detail);
        return super.handleExceptionInternal(e, body, headers, code.getHttpStatus(), request);
    }

    /**
     * 프로덕션 환경 여부 확인
     */
    private boolean isProductionProfile() {
        return "prod".equalsIgnoreCase(activeProfile);
    }
}
