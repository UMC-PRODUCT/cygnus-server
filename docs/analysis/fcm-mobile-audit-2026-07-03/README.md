# FCM 모바일 친화성/보안/확장성 감사 산출물

## 요약

- 대상: FCM 이벤트 기반 알림 구현
- 목적: iOS/Android 모바일 친화성, 보안/남용 리스크, 기능 개선점, 향후 확장성 평가
- 최종 판정: 기본 발송 파이프라인은 양호하지만, 플랫폼별 payload 계약과 보안 boundary 보강이 필요하다.
- 검토 범위와 실행 순서는 별도 evidence 문서에 기록한다.

## 문서

- [최종 감사 보고서](./report.md)
- [검토 실행 순서](./evidence/execution-sequence.md)

## 주요 결론

1. 모바일 친화성은 "기본 알림 표시 + data 전달" 수준이다. Android/iOS별 channel, click action/category, APNs badge/sound/content-available, TTL, collapse key 같은 플랫폼별 옵션이 서버 계약에 없다.
2. 보안상 추가로 보강할 항목은 관리자 target 크기·조합 정책, data/deepLink/imageUrl 정책, 인증 사용자 FCM token 등록 abuse 방어다.
3. 확장성은 현재 member/org audience에는 충분하지만, role/tag/dynamic segment/user preference 같은 새 대상 축에는 event/command/resolver 수정이 필요하다.
4. Domain Event는 공용 `event_outbox`에 항상 저장되며, relay 중지 시에도 `PENDING` 상태로 보존된다.
