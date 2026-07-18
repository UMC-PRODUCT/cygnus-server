# Form 도메인 (기존 문서 파일명: `survey.md`)

> 실제 구현 package는 `com.umc.product.form`이다. `survey`라는 파일명과 이전 온보딩 표현은 호환성을 위해 남아 있지만, 신규 코드·문서 링크·테스트 경로는 모두 `form`을 사용한다.

## 역할과 경계

Form engine은 폼(`Form`), 섹션, 질문, 선택지, 응답, 답변과 첨부 usage를 소유한다. 폼 생성·구조 변경·게시·응답 draft/submit/delete의 domain 불변식은 Form 안에 두고, Project/Notice/Feedback의 business 권한은 각 consumer가 `FormOwnerPolicy`로 제공한다.

Storage entity나 consumer aggregate를 직접 참조하지 않는다. 파일 첨부는 [FileUsageRegistry](storage.md)의 `form.answer/{answerId}/attachments` snapshot으로 동기화하고, 응답/폼 삭제 시 해당 owner usage를 먼저 detach한다.

## Ownership registry

ownership row는 authorization truth이고 기존 `formId`/consumer mapping은 navigation mirror다. row는 `(form_id, namespace, owner_resource_key, slot)`으로 insert-only immutable이며 같은 binding 재시도만 idempotent하다. 다른 owner로 transfer하거나 client가 좌표를 주입하는 경로는 없다. schema는 [V2026.07.16.00.10](../../../src/main/resources/db/migration/V2026.07.16.00.10__create_form_ownership_registry.sql), policy 결정은 [ADR-027](../../adr/027-engine-resource-ownership-namespaces.md)이다.

| namespace | source/owner key | slot | 관리/응답 원칙 |
| --- | --- | --- | --- |
| `project.application-form` | `project_application_form.project_id` | `default` | Project가 server-resolve한 owner만 구조 변경·게시·삭제·응답 결과 조회를 수행하고, Project의 공개/응답 정책을 함께 검사한다. |
| `notice.vote` | `notice_vote.notice_id` | `default` | Notice가 owner를 결정한다. 투표 기간·중복 투표 등 Notice 정책을 Form permission과 함께 검사한다. |
| `feedback.template` | `user_feedback_template.id` | `default` | template owner 정책과 respondent credential을 유지한다. |
| `form.standalone` | `form.id` | `default` | creator만 관리·게시·삭제·응답 결과 조회. published form은 공개 READ/RESPOND, draft는 creator만 READ다. |

backfill source와 duplicate-owner preflight는 [`FormOwnershipRolloutAdapter`](../../../src/main/java/com/umc/product/form/adapter/out/backfill/FormOwnershipRolloutAdapter.java), Project/Notice/Feedback mapping은 각 `*FormOwnershipBackfillSource.java`에서 확인한다. 같은 Form에 복수 owner 또는 같은 tuple에 복수 Form이 있으면 preflight가 hard-fail하며 임의의 row를 선택하지 않는다.

모든 root/child 조회와 mutation은 expected owner → persisted binding exact match → namespace evaluator → consumer business permission 순서로 평가한다. evaluator가 없거나 두 개이면 fail-closed한다. anonymous RESPOND/READ의 기존 access-key 계약은 유지하되 management와 `READ_RESPONSES`는 authenticated owner를 요구한다.

## 응답·첨부 lifecycle

- named 응답은 respondent member scope와 Form parent chain을 함께 확인한다.
- anonymous 응답은 raw key를 저장하지 않고 hash를 저장한다. null key, hash mismatch, named 응답에 anonymous key 사용은 동일한 forbidden/not-found 경계로 수렴한다.
- anonymous attachment의 `null` update는 기존 Answer ID와 legacy snapshot을 보존한다. 빈 배열은 clear, 기존 subset은 부분 제거만 허용한다. 서버가 보지 못한 새 attachment를 익명 요청이 추가할 수 없다.
- FILE Answer 저장/수정/삭제와 `file_usage` 변경은 한 transaction에서 수행한다. usage 등록 실패 시 Answer/응답 변경도 rollback한다.
- 마지막 response/form 삭제에서 usage가 0이 되면 Storage가 `unreferenced_at`을 기록하고, cleanup은 registry `READY`와 retention을 모두 통과한 뒤에만 가능하다. 실제 물리 삭제는 [Storage runbook](../database-backfill-with-replicas.md)의 claim/CAS 절차를 따른다.

## 공개 계약과 운영 주의

기존 REST/GraphQL 요청의 form/response 동작을 유지하며 namespace, owner resource key, raw engine ID를 새 외부 필드로 노출하지 않는다. Form core에 Project·Notice·Feedback 분기를 추가하지 말고 consumer policy adapter를 확장한다. strict enforcement는 `app.engine-ownership.enforcement-enabled=false`에서 시작해 세 registry `READY`, exact-one namespace coverage, final reconcile과 replica 검증 후에만 켠다.

## 코드와 테스트 진입점

- domain/application: [`Form.java`](../../../src/main/java/com/umc/product/form/domain/Form.java), [`FormOwnershipAccessService`](../../../src/main/java/com/umc/product/form/application/service/FormOwnershipAccessService.java), [`FormOwnerPolicyRegistry`](../../../src/main/java/com/umc/product/form/application/service/FormOwnerPolicyRegistry.java)
- persistence/backfill: [`FormOwnershipPersistenceAdapter`](../../../src/main/java/com/umc/product/form/adapter/out/persistence/FormOwnershipPersistenceAdapter.java), [`FormOwnershipRolloutAdapter`](../../../src/main/java/com/umc/product/form/adapter/out/backfill/FormOwnershipRolloutAdapter.java)
- 테스트: [`Form 테스트`](../test/survey.md) (실제 package `com.umc.product.form`)
