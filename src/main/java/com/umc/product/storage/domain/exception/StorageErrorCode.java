package com.umc.product.storage.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StorageErrorCode implements BaseCode {

    // 파일 관련 에러
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORAGE-0001", "파일을 찾을 수 없어요. 선택한 파일을 확인해주세요."),
    FILE_UPLOAD_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "STORAGE-0002", "파일 업로드가 아직 끝나지 않았어요. 업로드를 완료한 뒤 다시 시도해주세요."),
    FILE_ALREADY_UPLOADED(HttpStatus.BAD_REQUEST, "STORAGE-0003", "이미 업로드가 끝난 파일이에요. 파일 정보를 확인해주세요."),
    FILE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "STORAGE-0013",
        "파일을 삭제할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    FILE_USE_FORBIDDEN(HttpStatus.FORBIDDEN, "STORAGE-0016", "이 파일을 사용할 권한이 없어요. 본인이 업로드한 파일만 사용할 수 있어요."),

    // 파일 검증 에러
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "STORAGE-0004", "지원하지 않는 파일 형식이에요. 다른 파일을 선택해주세요."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "STORAGE-0005", "파일 크기가 너무 커요. 더 작은 파일을 선택해주세요."),
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "STORAGE-0006", "파일 형식 정보가 올바르지 않아요. 파일을 다시 선택해주세요."),
    FILE_SIZE_MISMATCH(HttpStatus.BAD_REQUEST, "STORAGE-0014", "요청한 파일 크기와 실제 업로드된 파일 크기가 달라요. 다시 업로드해주세요."),

    // 스토리지 에러
    STORAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE-0007", "파일을 업로드하지 못했어요. 잠시 후 다시 시도해주세요."),
    STORAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE-0008", "파일을 삭제하지 못했어요. 잠시 후 다시 시도해주세요."),
    STORAGE_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE-0009", "파일 접근 링크를 만들지 못했어요. 잠시 후 다시 시도해주세요."),
    STORAGE_METADATA_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE-0015",
        "파일 정보를 확인하지 못했어요. 잠시 후 다시 시도해주세요."),

    // CDN 에러
    CDN_SIGNING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE-0010", "CDN 접근 링크를 만들지 못했어요. 관리자에게 문의해주세요."),
    NO_ENV_KEYS(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE-0011", "CDN 설정이 누락됐어요. 관리자에게 문의해주세요."),
    INVALID_SPRING_PROFILE(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE-0012",
        "서버 실행 환경이 올바르지 않아요. 관리자에게 문의해주세요."),
    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
