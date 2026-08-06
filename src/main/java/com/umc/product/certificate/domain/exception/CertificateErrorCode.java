package com.umc.product.certificate.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CertificateErrorCode implements BaseCode {
    CERTIFICATE_NOT_FOUND(HttpStatus.NOT_FOUND, "CERTIFICATE-0001", "인증서를 찾을 수 없어요."),
    CERTIFICATE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "CERTIFICATE-0002", "인증서에 접근할 권한이 없어요."),
    CERTIFICATE_ISSUE_FORBIDDEN(HttpStatus.FORBIDDEN, "CERTIFICATE-0003", "인증서를 발급할 권한이 없어요."),
    CERTIFICATE_SELF_ISSUE_FORBIDDEN(HttpStatus.BAD_REQUEST, "CERTIFICATE-0004", "직접 발급할 수 없는 인증서 종류예요."),
    CERTIFICATE_ELIGIBILITY_NOT_MET(HttpStatus.BAD_REQUEST, "CERTIFICATE-0005", "인증서 발급 조건을 만족하지 않아요."),
    CERTIFICATE_ALREADY_REVOKED(HttpStatus.BAD_REQUEST, "CERTIFICATE-0006", "이미 폐기된 인증서예요."),
    CERTIFICATE_EXPIRED_OR_REVOKED(HttpStatus.BAD_REQUEST, "CERTIFICATE-0007", "만료되었거나 폐기된 인증서예요."),
    CERTIFICATE_SERIAL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CERTIFICATE-0008", "인증서 일련번호를 만들지 못했어요."),
    CERTIFICATE_RENDER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CERTIFICATE-0009", "인증서 PDF를 만들지 못했어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
