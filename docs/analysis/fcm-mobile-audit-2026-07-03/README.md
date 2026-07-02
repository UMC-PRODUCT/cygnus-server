# FCM 모바일 친화성/보안/확장성 감사 산출물

## 요약

- 대상: FCM 이벤트 기반 알림 구현
- 목적: iOS/Android 모바일 친화성, 보안/남용 리스크, 기능 개선점, 향후 확장성 평가
- 최종 판정: 기본 발송 파이프라인은 양호하지만, 플랫폼별 payload 계약과 보안 boundary 보강이 필요하다.
- 검증 상태: ULW 기준 `C001/C002/C003` 모두 `pass`, code re-review `APPROVE`, manual QA `passed`, final gate `APPROVE`

## 문서

- [최종 감사 보고서](./report.md)
- [모바일 payload 근거](./evidence/C001-mobile-payload.txt)
- [보안/남용 리스크 근거](./evidence/C002-security.txt)
- [확장성/운영성 근거](./evidence/C003-extensibility.txt)
- [코드 재검토 결과](./evidence/code-review.md)
- [Manual QA Matrix](./evidence/manual-qa.md)
- [Final Gate Review](./evidence/final-gate-review.md)
- [Quality Gate JSON](./evidence/quality-gate.json)
- [ULW Goals Snapshot](./evidence/ulw-goals.json)

## 주요 결론

1. 모바일 친화성은 "기본 알림 표시 + data 전달" 수준이다. Android/iOS별 channel, click action/category, APNs badge/sound/content-available, TTL, collapse key 같은 플랫폼별 옵션이 서버 계약에 없다.
2. 보안상 반드시 보강할 항목은 관리자 target validation, data/deepLink/imageUrl 정책, 인증 사용자 FCM token 등록 abuse 방어다.
3. 확장성은 현재 member/org audience에는 충분하지만, role/tag/dynamic segment/user preference 같은 새 대상 축에는 event/command/resolver 수정이 필요하다.
4. `event_outbox` 기반 durable queue 장점은 `EVENT_OUTBOX_ENABLED=true` 환경에 한정된다.

## Cleanup

감사 과정에서 서버, tmux, browser, container를 실행하지 않았다.
