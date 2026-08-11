package com.umc.product.inhouse.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InhouseErrorCode implements BaseCode {

    UMC_PRODUCT_MEMBER_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0001", "UMC PRODUCT 인원은 필수입니다."),
    UMC_PRODUCT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "INHOUSE-0002", "UMC PRODUCT 인원을 찾을 수 없습니다."),
    UMC_PRODUCT_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "INHOUSE-0003", "이미 등록된 UMC PRODUCT 인원입니다."),
    UMC_PRODUCT_MEMBER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0004", "회원 ID는 필수입니다."),
    UMC_PRODUCT_ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0005", "UMC PRODUCT 직책은 필수입니다."),
    UMC_PRODUCT_POSITION_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0006", "UMC PRODUCT 포지션은 필수입니다."),
    UMC_PRODUCT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "INHOUSE-0007", "UMC PRODUCT 관리 권한이 없습니다."),
    UMC_PRODUCT_DEPARTMENT_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0008", "UMC PRODUCT Department는 필수입니다."),
    UMC_PRODUCT_DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "INHOUSE-0009", "UMC PRODUCT Department를 찾을 수 없습니다."),
    UMC_PRODUCT_DEPARTMENT_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0010", "UMC PRODUCT Department 코드는 필수입니다."),
    UMC_PRODUCT_DEPARTMENT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0011", "UMC PRODUCT Department 이름은 필수입니다."),
    UMC_PRODUCT_START_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0012", "UMC PRODUCT 활동 시작일은 필수입니다."),
    UMC_PRODUCT_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "INHOUSE-0013", "UMC PRODUCT 활동 종료일은 시작일보다 빠를 수 없습니다."),
    UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0014", "UMC PRODUCT 멤버 활동 기간은 필수입니다."),
    UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "INHOUSE-0015",
        "활동 기간은 멤버 활동 기간과 상위 활동 기간 안에 있어야 합니다."),
    UMC_PRODUCT_ACTIVITY_PERIOD_NOT_FOUND(HttpStatus.NOT_FOUND, "INHOUSE-0016",
        "UMC PRODUCT 멤버 활동 기간을 찾을 수 없습니다."),
    UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED(HttpStatus.CONFLICT, "INHOUSE-0017",
        "UMC PRODUCT 멤버 활동 기간은 겹치거나 빈 날짜 없이 이어질 수 없습니다."),
    UMC_PRODUCT_ACTIVITY_PERIOD_HAS_ASSOCIATIONS(HttpStatus.CONFLICT, "INHOUSE-0018",
        "연결된 활동 이력이 있어 멤버 활동 기간을 삭제할 수 없습니다."),
    UMC_PRODUCT_CHAPTER_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0019", "UMC PRODUCT Chapter는 필수입니다."),
    UMC_PRODUCT_CHAPTER_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0020", "UMC PRODUCT Chapter 코드는 필수입니다."),
    UMC_PRODUCT_CHAPTER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0021", "UMC PRODUCT Chapter 이름은 필수입니다."),
    UMC_PRODUCT_CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND, "INHOUSE-0022", "UMC PRODUCT Chapter를 찾을 수 없습니다."),
    UMC_PRODUCT_CHAPTER_ALREADY_EXISTS(HttpStatus.CONFLICT, "INHOUSE-0023", "이미 존재하는 UMC PRODUCT Chapter 코드입니다."),
    UMC_PRODUCT_CHAPTER_HAS_MEMBERSHIPS(HttpStatus.CONFLICT, "INHOUSE-0024",
        "연결된 소속 이력이 있어 UMC PRODUCT Chapter를 삭제할 수 없습니다."),
    UMC_PRODUCT_CHAPTER_MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "INHOUSE-0025",
        "UMC PRODUCT Chapter 소속 이력을 찾을 수 없습니다."),
    UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED(HttpStatus.CONFLICT, "INHOUSE-0026",
        "동일한 UMC PRODUCT Chapter 소속 활동 기간이 겹칩니다."),
    UMC_PRODUCT_LEADERSHIP_ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0027", "UMC PRODUCT Leadership 역할은 필수입니다."),
    UMC_PRODUCT_LEADERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "INHOUSE-0028", "UMC PRODUCT Leadership 이력을 찾을 수 없습니다."),
    UMC_PRODUCT_LEADERSHIP_OVERLAPPED(HttpStatus.CONFLICT, "INHOUSE-0029",
        "해당 기간에 중복되는 UMC PRODUCT Leadership이 존재합니다."),
    UMC_PRODUCT_DEPARTMENT_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "INHOUSE-0030",
        "UMC PRODUCT Department 참여 이력을 찾을 수 없습니다."),
    UMC_PRODUCT_DEPARTMENT_PARTICIPATION_OVERLAPPED(HttpStatus.CONFLICT, "INHOUSE-0031",
        "동일 멤버의 UMC PRODUCT Department 참여 기간이 겹칩니다."),
    UMC_PRODUCT_DEPARTMENT_LEAD_OVERLAPPED(HttpStatus.CONFLICT, "INHOUSE-0032",
        "해당 기간에 이미 UMC PRODUCT Department Lead가 존재합니다."),
    UMC_PRODUCT_DEPARTMENT_HAS_PARTICIPANTS(HttpStatus.CONFLICT, "INHOUSE-0033",
        "연결된 참여 이력이 있어 UMC PRODUCT Department를 삭제할 수 없습니다."),
    UMC_PRODUCT_DEPARTMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "INHOUSE-0034", "이미 존재하는 UMC PRODUCT Department 코드입니다."),
    UMC_PRODUCT_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "INHOUSE-0035", "이미 다른 UMC PRODUCT 인원에 연동된 계정입니다."),
    UMC_PRODUCT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "INHOUSE-0036", "UMC PRODUCT 인원의 계정 연동을 찾을 수 없습니다."),
    UMC_PRODUCT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0037", "UMC PRODUCT 인원 이름은 필수입니다."),
    UMC_PRODUCT_NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0038", "UMC PRODUCT 인원 닉네임은 필수입니다."),
    UMC_PRODUCT_ACCOUNT_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "INHOUSE-0039", "UMC PRODUCT 계정 유형은 필수입니다."),
    UMC_PRODUCT_DEPARTMENT_CYCLE(HttpStatus.BAD_REQUEST, "INHOUSE-0040", "UMC PRODUCT Department 상하 관계에 순환이 생길 수 없습니다."),
    UMC_PRODUCT_DEPARTMENT_HAS_CHILDREN(HttpStatus.CONFLICT, "INHOUSE-0041", "하위 Department가 있어 삭제할 수 없습니다."),
    UMC_PRODUCT_ENGLISH_NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "INHOUSE-0042",
        "영어 닉네임은 소문자·숫자·._- 2~30자여야 합니다."),
    UMC_PRODUCT_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "INHOUSE-0043", "이미 사용 중인 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
