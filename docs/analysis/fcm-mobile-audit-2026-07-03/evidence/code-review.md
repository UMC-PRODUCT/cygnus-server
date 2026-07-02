# FCM Mobile Audit Re-Review

## Verdict

- codeQualityStatus: WATCH
- recommendation: APPROVE
- user-facing recommendation: APPROVE
- reportPath: `.omo/evidence/fcm-mobile-audit-rereview-code-review.md`
- blockers: 없음

## Skill Perspective Check

- `omo:remove-ai-slops`: 로드 완료. 변경된 테스트/프로덕션 코드가 없는 감사 산출물이므로 deletion-only test, tautological test, 구현 상수 미러링, 불필요한 production parsing/normalization 위반은 발견되지 않았습니다.
- `omo:programming`: 로드 완료. Java 전용 reference는 이 스킬에 없지만 boundary parsing, 근거 없는 validation, 구현 미러링 테스트, 불필요한 추상화 관점으로 적용했습니다. 코드 변경은 없으며, 감사 산출물이 위 관점을 위반한다고 볼 중대 사유는 없습니다.

## CRITICAL

- 없음.

## HIGH

- 없음.

## MEDIUM

- 없음.

## LOW

- `.omo/evidence/fcm-mobile-audit-code-review.md:5`는 이전 차단 리뷰 결과(`BLOCK`/`REQUEST_CHANGES`)를 그대로 담고 있습니다. 현재 최종 보고서와 C002/C003 evidence가 해당 blocker를 보강했으므로, 사용자 기준인 "material unsupported claims or missed severe risks"에는 해당하지 않습니다.
- `.omo/ulw-loop/fcm-mobile-audit-20260701/goals.json:14`의 goal status는 여전히 `in_progress`입니다. 각 success criterion은 pass로 갱신되어 있고 산출물 경로도 존재하므로 이번 재검토의 blocker로 보지는 않았습니다.

## Verified Claims

- 토큰 등록 abuse 리스크는 최종 보고서에 반영되어 있고 코드와 맞습니다. `FcmRegistrationRequest`의 `fcmToken`은 `@NotBlank`만 있으며, `FcmService.registerFcmToken`은 같은 회원의 새로운 토큰을 새 활성 토큰으로 저장할 수 있고 사용자별 활성 토큰 cap은 확인되지 않았습니다.
- `event_outbox` 표현은 조건부로 정정되었습니다. `application.yml` 기본값은 `EVENT_OUTBOX_ENABLED:false`이고, `OutboxDomainEventPublisher`는 true일 때만 활성화되며 기본 경로는 `SpringDomainEventPublisher`입니다.
- 모바일 payload 평가는 코드와 Firebase 공식 문서 방향에 부합합니다. 현재 어댑터는 공통 `Notification(title/body/image)`와 `data.deepLink`만 구성하고 Android/APNs config, TTL, click action/category, mutable-content/content-available 같은 플랫폼 옵션을 설정하지 않습니다.
- 관리자 발송 target/data/deepLink 검증 gap, token plaintext 저장 주의, token validation scheduler 다중 인스턴스 주의, FirebaseApp 초기화 리스크는 모두 코드/설정 근거가 있으며 중대한 과장으로 보이지 않습니다.

## Evidence Reviewed

- `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/fcm-mobile-audit-report.md`
- `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/C001-mobile-payload.txt`
- `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/C002-security.txt`
- `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/C003-extensibility.txt`
- `.omo/evidence/fcm-mobile-audit-manual-qa.md`
- `.omo/evidence/fcm-mobile-audit-code-review.md`
- `.omo/ulw-loop/fcm-mobile-audit-20260701/goals.json`
- Source cross-check: FCM controller/request DTOs, FCM service/domain/adapters, event outbox publishers/config, token validation scheduler/service.
- Firebase official docs cross-check: message types, Android receive behavior, Apple silent notifications, cross-platform Android/APNs options.
