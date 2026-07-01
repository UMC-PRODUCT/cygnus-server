package com.umc.product.recruiting.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RecruitingErrorCode implements BaseCode {

    RECRUITING_SEASON_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITING-0001", "모집 시즌을 찾을 수 없어요. 모집 정보를 확인해주세요."),
    RECRUITING_ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITING-0002", "모집 차수를 찾을 수 없어요. 모집 차수를 확인해주세요."),
    RECRUITING_APPLICATION_FORM_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITING-0003", "지원 폼을 찾을 수 없어요. 지원 폼을 확인해주세요."),
    RECRUITING_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITING-0004", "지원서를 찾을 수 없어요. 지원서 정보를 확인해주세요."),
    RECRUITING_EVALUATION_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITING-0005", "평가 템플릿을 찾을 수 없어요."),
    RECRUITING_INTERVIEW_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITING-0006", "면접 배정을 찾을 수 없어요."),
    RECRUITING_INTERVIEW_EVALUATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITING-0007", "면접 평가를 찾을 수 없어요."),
    RECRUITING_ROUND_INVALID_ROUND_NO(HttpStatus.BAD_REQUEST, "RECRUITING-0100", "추가모집 차수는 1 이상이어야 해요."),
    RECRUITING_SEASON_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "RECRUITING-0101", "현재 모집 시즌 상태에서는 할 수 없는 작업이에요."),
    RECRUITING_ROUND_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "RECRUITING-0102", "현재 모집 차수 상태에서는 할 수 없는 작업이에요."),
    RECRUITING_SEASON_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECRUITING-0103", "이미 같은 학교와 기수의 모집 시즌이 있어요."),
    RECRUITING_ROUND_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECRUITING-0104", "이미 같은 시즌의 모집 차수가 있어요."),
    RECRUITING_APPLICATION_FORM_TRACK_REQUIRED(HttpStatus.BAD_REQUEST, "RECRUITING-0200", "지원 폼의 모집 트랙을 선택해주세요."),
    RECRUITING_APPLICATION_FORM_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "RECRUITING-0201", "현재 지원 폼 상태에서는 할 수 없는 작업이에요."),
    RECRUITING_APPLICATION_FORM_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECRUITING-0202", "이미 해당 모집 차수에 연결된 지원 폼이에요."),
    RECRUITING_APPLICATION_FORM_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "RECRUITING-0203", "게시된 지원 폼에만 지원할 수 있어요."),
    RECRUITING_APPLICATION_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "RECRUITING-0300", "현재 지원서 상태에서는 할 수 없는 작업이에요."),
    RECRUITING_APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECRUITING-0301", "이미 같은 모집 차수에 제출한 지원서가 있어요."),
    RECRUITING_APPLICATION_DIFFERENT_SCHOOL_EXISTS(HttpStatus.CONFLICT, "RECRUITING-0302", "같은 기수의 다른 학교 모집에 이미 지원했어요."),
    RECRUITING_APPLICATION_REAPPLICATION_BLOCKED(HttpStatus.CONFLICT, "RECRUITING-0303", "진행 중이거나 합격한 지원서가 있어 재지원할 수 없어요."),
    RECRUITING_APPLICATION_FINAL_PASS_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECRUITING-0304", "이미 최종 합격한 지원서가 있어요."),
    RECRUITING_REGISTRATION_FORBIDDEN(HttpStatus.FORBIDDEN, "RECRUITING-0305", "중앙운영사무국 총괄단 이상만 최종 등록을 확정할 수 있어요."),
    RECRUITING_APPLICATION_MEMBER_REQUIRED(HttpStatus.BAD_REQUEST, "RECRUITING-0306", "챌린저 등록에는 연결된 회원 정보가 필요해요."),
    RECRUITING_EVALUATION_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "RECRUITING-0400", "현재 평가 상태에서는 할 수 없는 작업이에요."),
    RECRUITING_EVALUATION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "RECRUITING-0401", "이미 제출한 지원자 평가가 있어요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
