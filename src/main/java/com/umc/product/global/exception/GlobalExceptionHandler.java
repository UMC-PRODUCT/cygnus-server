package com.umc.product.global.exception;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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

    @ExceptionHandler(value = BusinessException.class)
    public ResponseEntity<Object> onThrowException(BusinessException e, WebRequest request) {
        log.warn("[BUSINESS EXCEPTION] domain={}, code={}, message={}", e.getDomain(), e.getBaseCode().getCode(),
            e.getMessage(), e);

        ApiResponse<Object> body = BusinessExceptionResponseResolver.toApiResponse(e);
        return super.handleExceptionInternal(
            e,
            body,
            HttpHeaders.EMPTY,
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

    /**
     * 프로덕션 환경 여부 확인
     */
    private boolean isProductionProfile() {
        return "prod".equalsIgnoreCase(activeProfile);
    }
}
