package com.umc.product.curriculum.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CurriculumErrorCode implements BaseCode {

    CURRICULUM_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM-0001", "커리큘럼을 찾을 수 없어요. 선택한 커리큘럼을 확인해주세요."),
    WORKBOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM-0002", "워크북을 찾을 수 없어요. 선택한 워크북을 확인해주세요."),
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM-0003", "미션을 찾을 수 없어요. 선택한 미션을 확인해주세요."),
    WORKBOOK_HAS_SUBMISSIONS(HttpStatus.CONFLICT, "CURRICULUM-0004", "제출된 워크북이 있어 삭제할 수 없어요. 제출 내역을 먼저 확인해주세요."),
    WORKBOOK_NOT_IN_CURRICULUM(HttpStatus.NOT_FOUND, "CURRICULUM-0005", "이 커리큘럼에 포함된 워크북이 아니에요. 워크북을 다시 선택해주세요."),
    CHALLENGER_WORKBOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM-0006", "챌린저 워크북을 찾을 수 없어요. 선택한 워크북을 확인해주세요."),
    SUBMISSION_REQUIRED(HttpStatus.BAD_REQUEST, "CURRICULUM-0007", "제출 내용을 입력해주세요."),
    INVALID_WORKBOOK_STATUS(HttpStatus.BAD_REQUEST, "CURRICULUM-0008", "워크북 상태가 올바르지 않아요. 상태 값을 확인해주세요."),
    WORKBOOK_SUBMISSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "CURRICULUM-0009", "이미 해당 주차의 워크북 미션을 제출했어요. 제출 내역을 확인해주세요."),
    CURRICULUM_ALREADY_EXISTS(HttpStatus.CONFLICT, "CURRICULUM-0010", "해당 기수와 파트의 커리큘럼이 이미 있어요. 기존 커리큘럼을 확인해주세요."),
    WORKBOOK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CURRICULUM-0011",
        "이 워크북에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    INVALID_WEEKLY_CURRICULUM_PERIOD(HttpStatus.BAD_REQUEST, "CURRICULUM-0012", "주차 커리큘럼 시작일은 종료일보다 빨라야 해요. 기간을 다시 선택해주세요."),
    INVALID_WORKBOOK_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "CURRICULUM-0013", "현재 상태에서는 워크북 상태를 변경할 수 없어요. 상태를 확인해주세요."),
    WEEKLY_CURRICULUM_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM-0014", "주차별 커리큘럼을 찾을 수 없어요. 선택한 주차를 확인해주세요."),
    CURRICULUM_HAS_WEEKLY_CURRICULUMS(HttpStatus.CONFLICT, "CURRICULUM-0015", "주차별 커리큘럼이 남아 있어 삭제할 수 없어요. 주차별 커리큘럼을 먼저 정리해주세요."),
    WEEKLY_CURRICULUM_HAS_WORKBOOKS(HttpStatus.CONFLICT, "CURRICULUM-0016", "원본 워크북이 남아 있어 삭제할 수 없어요. 원본 워크북을 먼저 정리해주세요."),
    WEEKLY_CURRICULUM_DATE_LOCKED(HttpStatus.CONFLICT, "CURRICULUM-0017", "배포된 워크북이 있어 주차 기간을 수정할 수 없어요. 배포 상태를 확인해주세요."),
    WEEKLY_CURRICULUM_ALREADY_EXISTS(HttpStatus.CONFLICT, "CURRICULUM-0018", "동일한 주차와 부록 여부의 주차별 커리큘럼이 이미 있어요. 기존 항목을 확인해주세요."),
    WEEKLY_CURRICULUM_PERIOD_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "CURRICULUM-0019", "종료된 기간으로는 주차별 커리큘럼을 만들거나 수정할 수 없어요. 기간을 다시 선택해주세요."),
    MISSION_HAS_SUBMISSIONS(HttpStatus.CONFLICT, "CURRICULUM-0020", "이미 제출된 미션이 있어 삭제할 수 없어요. 제출 내역을 먼저 확인해주세요."),
    RELEASED_WORKBOOK_NECESSARY_MISSION_FORBIDDEN(HttpStatus.BAD_REQUEST, "CURRICULUM-0021", "배포된 워크북에는 필수 미션을 추가할 수 없어요. 선택 미션으로 추가해주세요."),
    RELEASED_WORKBOOK_MISSION_UPGRADE_FORBIDDEN(HttpStatus.BAD_REQUEST, "CURRICULUM-0022", "배포된 워크북의 미션은 필수에서 선택으로만 변경할 수 있어요."),
    MISSION_SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM-0023", "미션 제출물을 찾을 수 없어요."),
    MISSION_FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM-0024", "미션 피드백을 찾을 수 없어요."),
    FEEDBACK_REQUIRED(HttpStatus.BAD_REQUEST, "CURRICULUM-0025", "피드백 내용을 입력해주세요."),
    FEEDBACK_RESULT_REQUIRED(HttpStatus.BAD_REQUEST, "CURRICULUM-0026", "피드백 평가 결과를 선택해주세요."),
    SUBMISSION_PERIOD_ENDED(HttpStatus.CONFLICT, "CURRICULUM-0027", "주차별 커리큘럼 제출 기간이 종료되었어요."),
    SUBMISSION_EDIT_PERIOD_ENDED(HttpStatus.CONFLICT, "CURRICULUM-0028", "미션 제출물 수정 가능 기간이 종료되었어요."),
    MISSION_SUBMISSION_ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "CURRICULUM-0029", "이미 철회된 미션 제출물이에요."),
    FEEDBACK_EDIT_PERIOD_ENDED(HttpStatus.CONFLICT, "CURRICULUM-0030", "미션 피드백 수정 가능 기간이 종료되었어요."),
    FEEDBACK_DELETE_PERIOD_ENDED(HttpStatus.CONFLICT, "CURRICULUM-0031", "기수가 종료되어 미션 피드백을 삭제할 수 없어요."),
    STUDY_GROUP_NOT_MATCHED(HttpStatus.CONFLICT, "CURRICULUM-0032", "커리큘럼과 일치하는 스터디 그룹을 찾을 수 없어요."),
    WEEKLY_BEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "CURRICULUM-0033", "해당 그룹과 주차의 베스트 워크북이 이미 선정되었어요."),
    BEST_WORKBOOK_REQUIREMENTS_NOT_MET(HttpStatus.CONFLICT, "CURRICULUM-0034", "베스트 워크북 선정 조건을 충족하지 못했어요."),
    BEST_WORKBOOK_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "CURRICULUM-0035", "베스트 워크북 선정 사유를 입력해주세요."),
    CHALLENGER_WORKBOOK_ALREADY_EXISTS(HttpStatus.CONFLICT, "CURRICULUM-0036",
        "이미 배포된 챌린저 워크북이에요. 기존 워크북을 확인해주세요."),
    INVALID_CURRICULUM_LEARNING_TYPE(HttpStatus.BAD_REQUEST, "CURRICULUM-0037",
        "기수의 학습 방식에 맞는 파트 또는 트랙 하나를 선택해주세요."),
    CURRICULUM_TRACK_REQUIRED(HttpStatus.BAD_REQUEST, "CURRICULUM-0038",
        "조회할 기본 트랙을 선택해주세요."),
    UNSUPPORTED_CURRICULUM_TRACK(HttpStatus.BAD_REQUEST, "CURRICULUM-0039",
        "기본 트랙만 커리큘럼으로 운영할 수 있어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
