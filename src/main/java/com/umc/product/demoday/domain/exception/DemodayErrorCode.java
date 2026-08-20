package com.umc.product.demoday.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DemodayErrorCode implements BaseCode {

    DEMODAY_POLL_INVALID_WINDOW(HttpStatus.BAD_REQUEST, "DEMODAY-0100", "투표 시작 시각은 종료 시각보다 앞서야 해요."),
    DEMODAY_POLL_OPEN_AT_REQUIRED(HttpStatus.BAD_REQUEST, "DEMODAY-0103", "데모데이 투표 시작 시간을 입력해주세요"),
    DEMODAY_POLL_CLOSE_AT_REQUIRED(HttpStatus.BAD_REQUEST, "DEMODAY-0104", "데모데이 투표 종료 시간을 입력해주세요"),
    DEMODAY_POLL_BOOTH_LOCKED(HttpStatus.CONFLICT, "DEMODAY-0105", "투표가 시작되어 부스를 추가할 수 없어요."),
    DEMODAY_POLL_INVALID_NAME(HttpStatus.BAD_REQUEST, "DEMODAY-0106", "데모데이 이름은 1자 이상 100자 이하여야 해요."),
    DEMODAY_POLL_GISU_REQUIRED(HttpStatus.BAD_REQUEST, "DEMODAY-0107", "데모데이가 진행되는 기수를 입력해주세요."),
    DEMODAY_POLL_ALREADY_OPEN(HttpStatus.CONFLICT, "DEMODAY-0108", "이미 오픈된 데모데이 투표 행사 입니다."),
    DEMODAY_POLL_ALREADY_CLOSED(HttpStatus.CONFLICT, "DEMODAY-0109", "이미 종료된 데모데이 투표 행사 입니다."),
    DEMODAY_POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "DEMODAY-0110", "데모데이 투표 행사를 찾을 수 없습니다."),
    DEMODAY_POLL_NOT_OPEN(HttpStatus.NOT_FOUND, "DEMODAY-0111", "데모데이 투표 행사가 아직 시작되지 않았습니다."),
    DEMODAY_POLL_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "DEMODAY-0112", "허용되지 않는 데모데이 투표 상태 전이입니다."),

    DEMODAY_BOOTH_INVALID_IDENTIFIER(HttpStatus.BAD_REQUEST, "DEMODAY-0200", "부스는 등록된 프로젝트나 표시 이름 중 하나를 가져야 합니다."),
    DEMODAY_BOOTH_INVALID_NAME(HttpStatus.BAD_REQUEST, "DEMODAY-0201", "부스 이름은 1자 이상 255자 이하로 작성해주세요."),
    DEMODAY_BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, "DEMODAY-0202", "부스를 찾을 수 없습니다."),
    DEMODAY_BOOTH_INVALID_CODE(HttpStatus.BAD_REQUEST, "DEMODAY-0203", "부스 코드는 1 이상의 정수여야 합니다."),
    DEMODAY_BOOTH_CODE_DUPLICATED(HttpStatus.CONFLICT, "DEMODAY-0204", "같은 데모데이 투표에서 이미 사용 중인 부스 코드입니다."),

    DEMODAY_ENTRY_CODE_ALREADY_REDEEMED(HttpStatus.CONFLICT, "DEMODAY-0300", "이미 사용된 인증 코드예요."),
    DEMODAY_ENTRY_CODE_ALREADY_BOUND(HttpStatus.CONFLICT, "DEMODAY-0301", "이미 다른 계정에 연결된 인증 코드예요."),
    DEMODAY_ENTRY_CODE_GENERATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DEMODAY-0302", "행사 진행 전 또는 진행 중에만 생성할 수 있습니다"),
    DEMODAY_ENTRY_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "DEMODAY-0303", "존재하지 않는 입장 코드예요."),
    DEMODAY_ENTRY_CODE_POLL_MISMATCH(HttpStatus.CONFLICT, "DEMODAY-0304", "이번 데모데이의 입장 코드가 아니에요."),

    DEMODAY_VOTE_NOT_OPENED_YET(HttpStatus.CONFLICT, "DEMODAY-0400", "아직 투표 시간이 아니에요."),
    DEMODAY_VOTE_CLOSED(HttpStatus.CONFLICT, "DEMODAY-0401", "투표가 종료되었어요."),
    DEMODAY_VOTE_ALREADY_CAST(HttpStatus.CONFLICT, "DEMODAY-0402", "이미 투표를 완료했어요."),
    DEMODAY_VOTE_ALREADY_REVOKED(HttpStatus.CONFLICT, "DEMODAY-0403", "이미 무효 처리된 표에요"),
    DEMODAY_VOTE_POLL_MISMATCH(HttpStatus.CONFLICT, "DEMODAY-0404", "이번 데모데이의 부스에만 투표할 수 있어요."),
    DEMODAY_VOTE_QR_INVALID(HttpStatus.UNAUTHORIZED, "DEMODAY-0405", "QR 서명이 유효하지 않아요."),
    DEMODAY_VOTE_QR_EXPIRED(HttpStatus.UNAUTHORIZED, "DEMODAY-0406", "만료된 QR이에요. 다시 스캔해주세요."),
    DEMODAY_VOTE_QR_PURPOSE_MISMATCH(HttpStatus.UNAUTHORIZED, "DEMODAY-0407", "투표 인증 용도의 QR이 아니에요."),
    DEMODAY_VOTE_QR_POLL_MISMATCH(HttpStatus.UNAUTHORIZED, "DEMODAY-0408", "이번 데모데이의 QR이 아니에요."),
    DEMODAY_VOTE_INSUFFICIENT_STAMPS(HttpStatus.FORBIDDEN, "DEMODAY-0409", "투표하려면 스탬프 6개가 필요해요."),
    DEMODAY_VOTE_AUTHORIZATION_INVALID(HttpStatus.UNAUTHORIZED, "DEMODAY-0410", "투표 권한이 유효하지 않아요."),
    DEMODAY_VOTE_AUTHORIZATION_EXPIRED(HttpStatus.UNAUTHORIZED, "DEMODAY-0411", "투표 권한이 만료되었어요. INFO QR을 다시 스캔해주세요."),
    DEMODAY_VOTE_AUTHORIZATION_PURPOSE_MISMATCH(HttpStatus.UNAUTHORIZED, "DEMODAY-0412", "최종 투표 용도의 권한이 아니에요."),
    DEMODAY_VOTE_AUTHORIZATION_PARTICIPANT_MISMATCH(HttpStatus.FORBIDDEN, "DEMODAY-0413", "다른 참여자에게 발급된 투표 권한이에요."),
    DEMODAY_VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "DEMODAY-0414", "투표 기록을 찾을 수 없어요."),
    DEMODAY_VOTE_NOT_REVOKED(HttpStatus.CONFLICT, "DEMODAY-0415", "무효 처리되지 않은 표에요."),

    DEMODAY_STAMP_ALREADY_COLLECTED(HttpStatus.CONFLICT, "DEMODAY-0501", "이미 스탬프를 받은 부스예요."),
    DEMODAY_STAMP_ALREADY_REVOKED(HttpStatus.CONFLICT, "DEMODAY-0502", "이미 무효 처리된 스탬프예요."),
    DEMODAY_STAMP_POLL_MISMATCH(HttpStatus.CONFLICT, "DEMODAY-0503", "이번 데모데이의 부스에만 스탬프를 받을 수 있어요."),
    DEMODAY_STAMP_CREDENTIAL_INVALID(HttpStatus.CONFLICT, "DEMODAY-0504", "유효하지 않은 스탬프 QR이에요."),
    DEMODAY_STAMP_COOLDOWN_ACTIVE(HttpStatus.CONFLICT, "DEMODAY-0505", "아직 다음 스탬프를 적립할 수 없어요."),
    DEMODAY_STAMP_MAX_COUNT_REACHED(HttpStatus.CONFLICT, "DEMODAY-0506", "이미 모든 부스의 스탬프를 받았어요."),

    DEMODAY_ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DEMODAY-0600", "접근 권한이 없는 사용자입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
