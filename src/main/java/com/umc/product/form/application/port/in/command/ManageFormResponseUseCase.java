package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.command.dto.AnonymousFormResponseResult;
import com.umc.product.form.application.port.in.command.dto.CreateAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitAnonymousImmediatelyFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormResponseCommand;
import com.umc.product.form.domain.exception.FormErrorCode;

/**
 * FormResponse(폼 응답) 관리 UseCase.
 * <p>
 * 두 가지 응답 플로우를 지원한다:
 * <ol>
 *   <li><b>즉시 제출</b> (vote 등) — {@link #submitImmediately} / {@link #submitAnonymousImmediately} 한 번 호출.
 *       draft 없음. 이후 {@link #updateResponse} / {@link #deleteResponse} 또는
 *       {@link #updateAnonymousResponse} / {@link #deleteAnonymousResponse} 로 수정·취소 가능.</li>
 *   <li><b>draft 플로우</b> (지원서 등) — {@link #createDraft} / {@link #createAnonymousDraft} 로 시작,
 *       {@link #updateDraft} / {@link #updateAnonymousDraft} 로 임시저장 반복,
 *       {@link #submitDraft} / {@link #submitAnonymousDraft} 로 최종 제출.
 *       최종 제출 전에 {@link #deleteDraft} / {@link #deleteAnonymousDraft} 로 포기 가능.</li>
 * </ol>
 * <p>
 * <b>기명/익명 구분</b>: 각 플로우가 기명/익명 두 벌로 나뉜다.
 * <ul>
 *   <li><b>기명 응답</b> ({@code submitImmediately}, {@code updateResponse}, {@code deleteResponse},
 *       {@code createDraft}, {@code updateDraft}, {@code submitDraft}, {@code deleteDraft}):
 *       {@code respondentMemberId} (SUBMITTED 계열) 또는 {@code requesterMemberId} (DRAFT 계열) 필수.
 *       null 이거나 소유자와 다르면 예외.</li>
 *   <li><b>익명 응답</b> ({@code submitAnonymousImmediately}, {@code updateAnonymousResponse},
 *       {@code deleteAnonymousResponse}, {@code createAnonymousDraft}, {@code updateAnonymousDraft},
 *       {@code submitAnonymousDraft}, {@code deleteAnonymousDraft}):
 *       서버가 발급한 {@code responseAccessKey}(raw) 를 sha256 매칭으로 인증. UX 코드(예: 지원 코드) 는
 *       소비 도메인이 raw key 와 매핑해서 관리.</li>
 * </ul>
 * 상세 설계: docs/analysis/form-anonymous-response-design.md
 */
public interface ManageFormResponseUseCase {

    /**
     * (기명 전용) 폼에 대한 응답을 즉시 제출한다. (draft 없이 바로 SUBMITTED 상태 생성)
     * vote 같이 한 번에 제출하는 플로우에서 사용.
     * <p>
     * 결과 status 는 SUBMITTED라 제출 무결성 을 위해 형식 검증 + 필수 답변 누락 검증을 모두 수행.
     * 같은 폼에 이미 응답 (DRAFT/SUBMITTED 무관) 이 있으면 예외.
     *
     * @return 생성된 FormResponse ID
     */
    Long submitImmediately(SubmitFormResponseCommand command);

    /**
     * (기명 전용) 기존 SUBMITTED 응답의 답변을 전체 교체한다 (재제출 의미).
     * <p>
     * 결과 status 는 SUBMITTED 그대로 유지되므로 제출 무결성을 위해 필수 답변 누락 검증을 수행한다 ({@link #submitImmediately} 와 동일).
     * <p>
     * 작성 중인 응답을 갱신하려는 경우는 {@link #updateDraft} 사용.
     * 해당 폼에 대한 기존 SUBMITTED 응답이 없으면 예외.
     */
    void updateResponse(UpdateFormResponseCommand command);

    /**
     * (기명 전용) 본인이 제출한 SUBMITTED 응답을 삭제한다. (FormResponse + 연관 Answer 모두 삭제)
     * 삭제 후 다시 제출 가능. 기존 응답이 없으면 예외.
     * DRAFT 상태 응답 삭제는 {@link #deleteDraft} 사용.
     */
    void deleteResponse(DeleteFormResponseCommand command);

    /**
     * (기명 전용) 폼에 대한 draft 응답을 최초 생성한다. (빈 draft).
     * 이후 {@link #updateDraft} 로 답변을 채워나가고 {@link #submitDraft} 로 최종 제출.
     * 같은 폼에 이미 draft 또는 SUBMITTED 응답이 있으면 예외.
     *
     * @return 생성된 FormResponse ID
     */
    Long createDraft(CreateDraftFormResponseCommand command);

    /**
     * (기명 전용) 기존 draft 응답의 답변을 전체 교체한다 (작성 중 임시저장).
     * <p>
     * 결과 status 는 DRAFT 그대로 유지되며, 작성 중이라는 의미상 필수 답변 누락 허용.
     * 형식 / 옵션 소속 / OTHER 텍스트 등 형식 검증 만 수행한다.
     * 필수 누락은 {@link #submitDraft} 시점에 검증.
     * <p>
     * draft가 아닌 응답(SUBMITTED) 또는 존재하지 않는 응답 ID면 예외.
     * {@code requesterMemberId} 가 draft 소유자와 일치해야 한다.
     * 익명 draft 이거나, 소유자와 다르거나, null 이면 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN} 예외.
     * 익명 draft 조작은 {@link #updateAnonymousDraft} 사용.
     */
    void updateDraft(UpdateDraftFormResponseCommand command);

    /**
     * (기명 전용) draft 응답을 SUBMITTED 로 전환(최종 제출)한다.
     * <p>
     * 답변 내용은 이전 {@link #updateDraft} 로 저장된 값 그대로 유지 — status 만 DRAFT -> SUBMITTED.
     * 결과 status 가 SUBMITTED 가 되므로 저장된 답변 기준으로 필수 답변 누락 검증을 수행.
     * <p>
     * draft 가 아닌 응답이면 예외.
     * {@code requesterMemberId} 가 draft 소유자와 일치해야 한다.
     * 익명 draft 이거나, 소유자와 다르거나, null 이면 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN} 예외.
     * 익명 draft 조작은 {@link #submitAnonymousDraft} 사용.
     */
    void submitDraft(SubmitDraftFormResponseCommand command);

    /**
     * (기명 전용) draft 응답을 삭제한다. (연관 Answer 포함)
     * SUBMITTED 상태인 응답을 이 메서드로 삭제하면 예외 — SUBMITTED 삭제는 {@link #deleteResponse} 사용.
     * <p>
     * {@code requesterMemberId} 가 draft 소유자와 일치해야 한다.
     * 익명 draft 이거나, 소유자와 다르거나, null 이면 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN} 예외.
     * 익명 draft 조작은 {@link #deleteAnonymousDraft} 사용.
     */
    void deleteDraft(DeleteDraftFormResponseCommand command);

    /**
     * (익명 전용) 폼에 대한 익명 응답을 즉시 제출한다. (draft 없이 바로 SUBMITTED 상태 생성)
     * Vote 같이 한 번에 제출하는 익명 플로우에서 사용.
     * <p>
     * 결과 status 는 SUBMITTED라 제출 무결성을 위해 형식 검증 + 필수 답변 누락 검증을 모두 수행.
     * 서버가 랜덤 {@code responseAccessKey} 를 발급하고 sha256 해시만 저장한다.
     * 반환된 raw key 는 이후 익명 조작(update / delete) 시 재제출용.
     * <p>
     * 익명 응답의 중복 정책은 form 엔진에서 강제하지 않는다 — 소비 도메인 책임.
     */
    AnonymousFormResponseResult submitAnonymousImmediately(SubmitAnonymousImmediatelyFormResponseCommand command);

    /**
     * (익명 전용) 이미 SUBMITTED 상태인 익명 응답의 답변을 전체 교체한다 (재제출).
     * <p>
     * {@code responseAccessKey}(raw) 의 sha256 매칭으로 응답을 찾는다.
     * 매칭 실패, SUBMITTED 아님, 기명 응답인 경우 모두 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN}.
     * null 은 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     * <p>
     * 검증 정책은 기명 {@link #updateResponse} 와 동일 — 형식 + 필수 답변 누락 검증.
     */
    void updateAnonymousResponse(UpdateAnonymousFormResponseCommand command);

    /**
     * (익명 전용) 이미 SUBMITTED 상태인 익명 응답을 삭제한다. (FormResponse + 연관 Answer 모두 삭제)
     * <p>
     * {@code responseAccessKey}(raw) 의 sha256 매칭으로 응답을 찾는다.
     * 매칭 실패, SUBMITTED 아님, 기명 응답인 경우 모두 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN}.
     * null 은 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     * DRAFT 상태 익명 응답 삭제는 {@link #deleteAnonymousDraft} 사용.
     */
    void deleteAnonymousResponse(DeleteAnonymousFormResponseCommand command);

    /**
     * (익명 전용) 익명 draft 응답을 최초 생성한다.
     * <p>
     * 서버가 랜덤 {@code responseAccessKey} 를 발급하고 sha256 해시만 저장한다.
     * 반환된 raw key 는 이후 익명 조작(update / delete / submit) 시 재제출용.
     * <p>
     * 익명 응답의 중복 정책은 form 엔진에서 강제하지 않는다 — 소비 도메인 책임.
     */
    AnonymousFormResponseResult createAnonymousDraft(CreateAnonymousDraftFormResponseCommand command);

    /**
     * (익명 전용) 익명 draft 응답의 답변을 전체 교체한다 (익명 임시저장).
     * <p>
     * {@code responseAccessKey}(raw) 의 sha256 매칭으로 draft 를 찾는다.
     * 매칭 실패, DRAFT 아님, 기명 draft 인 경우 모두 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN}.
     * null 은 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     * <p>
     * 답변 검증 정책은 {@link #updateDraft} 와 동일 — 형식만, 필수 누락은 submit 시점 검증.
     */
    void updateAnonymousDraft(UpdateAnonymousDraftFormResponseCommand command);

    /**
     * (익명 전용) 익명 draft 응답을 SUBMITTED 로 전환(최종 제출)한다.
     * <p>
     * {@code responseAccessKey}(raw) 의 sha256 매칭으로 draft 를 찾는다.
     * 매칭 실패, DRAFT 아님, 기명 draft 인 경우 모두 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN}.
     * null 은 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     * <p>
     * 검증 정책은 {@link #submitDraft} 와 동일 — 저장된 답변 기준으로 필수 답변 누락 검증 수행.
     */
    void submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand command);

    /**
     * (익명 전용) 익명 draft 응답을 삭제한다. (연관 Answer 포함)
     * <p>
     * {@code responseAccessKey}(raw) 의 sha256 매칭으로 draft 를 찾는다.
     * 매칭 실패, DRAFT 아님, 기명 draft 인 경우 모두 {@link FormErrorCode#FORM_RESPONSE_FORBIDDEN}.
     * null 은 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     */
    void deleteAnonymousDraft(DeleteAnonymousDraftFormResponseCommand command);
}
