package com.umc.product.project.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectErrorCode implements BaseCode {

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT-0001", "프로젝트를 찾을 수 없어요. 선택한 프로젝트를 확인해주세요."),
    ALREADY_COMPLETED_PROJECT(HttpStatus.BAD_REQUEST, "PROJECT-0002", "이미 완료된 프로젝트예요. 프로젝트 상태를 확인해주세요."),
    PROJECT_ABORT_UNAVAILABLE(HttpStatus.BAD_REQUEST, "PROJECT-0003", "이 프로젝트는 중단할 수 없어요. 프로젝트 상태를 확인해주세요."),

    // ProjectApplication
    APPLICATION_NOT_SUBMITTED(HttpStatus.BAD_REQUEST, "PROJECT-0004", "제출된 지원서에서만 할 수 있는 작업이에요. 지원서 상태를 확인해주세요."),
    APPLICATION_SUBMIT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "PROJECT-0005", "이미 제출했거나 평가가 끝난 지원서예요. 지원서 상태를 확인해주세요."),
    APPLICATION_DRAFT_NOT_EXPOSABLE(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT-0019",
        "임시저장 지원서를 운영진 응답으로 보여줄 수 없어요. 관리자에게 문의해주세요."),
    APPLICATION_DRAFT_FILTER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PROJECT-0020",
        "운영진 지원자 목록에서는 임시저장 상태를 필터로 사용할 수 없어요. 다른 상태를 선택해주세요."),
    PROJECT_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT-0021", "지원서를 찾을 수 없어요. 선택한 지원서를 확인해주세요."),

    // ProjectApplicationForm
    APPLICATION_FORM_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT-0006", "프로젝트 지원 폼을 찾을 수 없어요. 선택한 프로젝트를 확인해주세요."),
    APPLICATION_FORM_ACCESS_NOT_ALLOWED(HttpStatus.FORBIDDEN, "PROJECT-0007",
        "이 지원 폼 섹션에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    APPLICATION_FORM_POLICY_PARTS_EMPTY(HttpStatus.BAD_REQUEST, "PROJECT-0013", "파트 섹션에는 파트를 1개 이상 선택해주세요."),
    APPLICATION_FORM_INVALID_SECTION_ID(HttpStatus.BAD_REQUEST, "PROJECT-0014", "현재 폼에 없는 섹션이에요. 섹션을 다시 선택해주세요."),
    APPLICATION_FORM_INVALID_QUESTION_ID(HttpStatus.BAD_REQUEST, "PROJECT-0015", "해당 섹션에 없는 질문이에요. 질문을 다시 선택해주세요."),
    APPLICATION_FORM_INVALID_OPTION_ID(HttpStatus.BAD_REQUEST, "PROJECT-0016", "해당 질문에 없는 선택지예요. 선택지를 다시 선택해주세요."),
    APPLICATION_FORM_OPTIONS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PROJECT-0017", "선택형 질문에만 선택지를 추가할 수 있어요. 질문 유형을 확인해주세요."),
    APPLICATION_FORM_OPTIONS_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT-0018", "선택형 질문에는 선택지가 1개 이상 필요해요. 선택지를 추가해주세요."),

    // Project Draft flow (PROJECT-101, 102, 107)
    PROJECT_DRAFT_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "PROJECT-0008", "작성 중인 프로젝트가 있어 새로 시작할 수 없어요. 기존 초안을 먼저 확인해주세요."),
    PROJECT_INVALID_STATE(HttpStatus.BAD_REQUEST, "PROJECT-0009", "현재 상태에서는 할 수 없는 작업이에요. 프로젝트 상태를 확인해주세요."),
    PROJECT_OWNER_NOT_PLAN_CHALLENGER(HttpStatus.BAD_REQUEST, "PROJECT-0010", "프로젝트 PO는 PLAN 파트 챌린저만 맡을 수 있어요. PO 정보를 확인해주세요."),
    PROJECT_SUBMIT_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "PROJECT-0011", "제출에 필요한 정보가 부족해요. 필수 항목을 확인해주세요."),
    PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PROJECT-0012",
        "이 프로젝트에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    PROJECT_DELETE_NOT_ALLOWED_IN_STATUS(HttpStatus.CONFLICT, "PROJECT-0022",
        "프로젝트는 DRAFT 또는 PENDING_REVIEW 상태에서만 삭제할 수 있어요. 상태를 확인해주세요."),
    PROJECT_ABORT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT-0023", "프로젝트 중단 사유를 입력해주세요."),
    PROJECT_TRANSFER_OWNERSHIP_APPLICATION_SCOPE_CONFLICT(HttpStatus.CONFLICT, "PROJECT-0024",
        "지원서가 연결된 프로젝트는 소유자 학교나 지부를 변경할 수 없어요."),

    // ProjectMember (PROJECT-003/004/005)
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT-0100", "프로젝트 멤버를 찾을 수 없어요. 멤버 목록을 확인해주세요."),
    PROJECT_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROJECT-0101", "이미 이 프로젝트의 멤버예요. 멤버 목록을 확인해주세요."),
    PROJECT_MAIN_PM_REMOVAL_REQUIRES_TRANSFER(HttpStatus.BAD_REQUEST, "PROJECT-0102", "메인 PM은 팀원 제거가 아니라 소유권 양도로 변경해주세요."),

    // ProjectPartQuota / Publish (PROJECT-105/108)
    PROJECT_PART_QUOTA_INVALID(HttpStatus.BAD_REQUEST, "PROJECT-0200", "파트 정원은 1명 이상으로 입력해주세요."),
    PROJECT_PART_QUOTA_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT-0202", "프로젝트를 공개하려면 파트별 정원을 1개 이상 등록해주세요."),
    PROJECT_PART_QUOTA_DUPLICATE(HttpStatus.BAD_REQUEST, "PROJECT-0203", "동일한 파트가 중복됐어요. 파트별 정원을 확인해주세요."),

    // ProjectMatchingRound
    PROJECT_MATCHING_ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT-0300", "매칭 차수를 찾을 수 없어요. 선택한 차수를 확인해주세요."),
    PROJECT_MATCHING_ROUND_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "PROJECT-0301",
        "매칭 차수 기간은 시작, 종료, 결정 마감 순서여야 해요. 시간을 다시 선택해주세요."),
    PROJECT_MATCHING_ROUND_PERIOD_OVERLAPPED(HttpStatus.CONFLICT, "PROJECT-0302",
        "같은 지부의 다른 매칭 차수와 기간이 겹쳐요. 기간을 다시 선택해주세요."),
    PROJECT_MATCHING_ROUND_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PROJECT-0303",
        "이 매칭 차수를 관리할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    PROJECT_MATCHING_ROUND_DELETE_CONFLICT(HttpStatus.CONFLICT, "PROJECT-0304",
        "연결된 지원서가 있는 매칭 차수는 삭제할 수 없어요. 지원서를 먼저 확인해주세요."),
    PROJECT_MATCHING_ROUND_TIME_REQUIRES_CHAPTER(HttpStatus.BAD_REQUEST, "PROJECT-0305",
        "시간 기준으로 조회하려면 지부를 함께 선택해주세요."),
    PROJECT_MATCHING_ROUND_LOCKED(HttpStatus.BAD_REQUEST, "PROJECT-0306",
        "결정 마감 시각이 지나 결정을 변경할 수 없어요. 차수 기간을 확인해주세요."),
    PROJECT_MATCHING_ROUND_NOT_ENDED(HttpStatus.BAD_REQUEST, "PROJECT-0310",
        "아직 지원 기간이 끝나지 않아 결정을 변경할 수 없어요. 지원 종료 시각을 확인해주세요."),
    PROJECT_MATCHING_ROUND_APPLICANTS_NOT_VIEWABLE(HttpStatus.BAD_REQUEST, "PROJECT-0311",
        "아직 지원 기간이 끝나지 않아 지원서를 조회할 수 없어요. 지원 종료 시각을 확인해주세요."),
    PROJECT_MATCHING_ROUND_NOT_FINALIZABLE(HttpStatus.BAD_REQUEST, "PROJECT-0307",
        "결정 마감 시각이 지난 뒤 자동 선발을 실행할 수 있어요. 마감 시각을 확인해주세요."),
    PROJECT_MATCHING_ROUND_POLICY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT-0308",
        "이 매칭 종류의 자동 선발 정책을 찾지 못했어요. 관리자에게 문의해주세요."),
    PROJECT_MATCHING_ROUND_PHASE_SEQUENCE_INVALID(HttpStatus.CONFLICT, "PROJECT-0309",
        "매칭 차수는 FIRST, SECOND, THIRD 순서로 배치하고 이전 차수 결정 마감 이후 1분 이상 간격을 둬야 해요."),
    PROJECT_MATCHING_ROUND_GISU_CHAPTER_MISMATCH(HttpStatus.BAD_REQUEST, "PROJECT-0312",
        "선택한 지부가 매칭 차수의 기수에 속하지 않아요. 기수와 지부를 다시 확인해주세요."),
    PROJECT_MATCHING_ROUND_OUTSIDE_GISU_PERIOD(HttpStatus.BAD_REQUEST, "PROJECT-0313",
        "매칭 차수 기간은 기수 활동 기간 안에 있어야 해요. 시간을 다시 선택해주세요."),

    // ProjectApplication (APPLY-001/002/003)
    PROJECT_DRAFT_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT-0204", "작성 중인 지원서를 찾을 수 없어요. 지원서 목록을 확인해주세요."),
    PROJECT_APPLICATION_PART_NOT_ALLOWED(HttpStatus.FORBIDDEN, "PROJECT-0205", "이 프로젝트에 지원할 수 있는 파트가 아니에요. 지원 가능한 파트를 확인해주세요."),
    PROJECT_APPLICATION_MEMBER_ALREADY_IN_TEAM(HttpStatus.CONFLICT, "PROJECT-0206", "이미 해당 기수에 소속된 팀이 있어 지원할 수 없어요. 팀 정보를 확인해주세요."),
    PROJECT_APPLICATION_DUPLICATE_SUBMISSION(HttpStatus.CONFLICT, "PROJECT-0207", "동일한 매칭 차수에 이미 제출한 지원서가 있어요. 기존 지원서를 확인해주세요."),
    PROJECT_APPLICATION_ROUND_NOT_OPEN(HttpStatus.BAD_REQUEST, "PROJECT-0208", "현재는 해당 매칭 차수의 지원 기간이 아니에요. 지원 기간을 확인해주세요."),
    PROJECT_APPLICATION_ROUND_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "PROJECT-0209", "선택한 매칭 차수가 내 파트와 맞지 않아요. 매칭 차수를 다시 선택해주세요."),
    PROJECT_APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROJECT-0210", "이미 작성 중인 지원서가 있어요. 기존 지원서를 이어서 작성해주세요."),
    PROJECT_APPLICATION_SELF_APPLY_NOT_ALLOWED(HttpStatus.FORBIDDEN, "PROJECT-0211", "내가 운영하는 프로젝트에는 지원할 수 없어요. 다른 프로젝트를 선택해주세요."),
    PROJECT_APPLICATION_DECISION_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "PROJECT-0212",
        "현재 상태에서는 합격 여부를 변경할 수 없어요. 지원서 상태를 확인해주세요."),
    PROJECT_APPLICATION_QUOTA_EXCEEDED(HttpStatus.CONFLICT, "PROJECT-0213",
        "해당 파트의 남은 자리를 초과해 합격 처리할 수 없어요. 파트 정원을 확인해주세요."),
    PROJECT_APPLICATION_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PROJECT-0214", "이미 종결된 지원서는 철회할 수 없어요. 지원서 상태를 확인해주세요."),
    PROJECT_APPLICATION_CANCEL_ROUND_CLOSED(HttpStatus.BAD_REQUEST, "PROJECT-0215", "매칭 차수가 종료되어 지원서를 철회할 수 없어요. 차수 기간을 확인해주세요."),
    PROJECT_APPLICATION_MINIMUM_SELECTION_REQUIRED(HttpStatus.CONFLICT, "PROJECT-0216",
        "매칭 규칙의 최소 선발 인원을 충족하지 않아 불합격 처리할 수 없어요. 합격 인원을 확인해주세요."),
    PROJECT_APPLICATION_ROUND_SCOPE_MISMATCH(HttpStatus.BAD_REQUEST, "PROJECT-0217",
        "프로젝트와 매칭 차수의 기수 또는 지부가 일치하지 않아요. 매칭 차수를 다시 선택해주세요."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
