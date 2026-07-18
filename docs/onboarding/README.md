# Onboarding

이 디렉터리는 새로 합류한 개발자가 UMC PRODUCT 서버의 주요 도메인, API 진입점, 운영 규칙을 빠르게 파악하기 위한 문서를 둔다.

## API 사용 문서

- [GraphQL Onboarding](graphql.md): GraphQL endpoint, GraphiQL, Apollo Sandbox, schema 확인, query 작성, resolver 구현 규칙

## 도메인 문서

- [Domain Onboarding](domain/README.md): 제품 도메인별 역할, 책임, 경계, UX 문구 기준
- [Database Backfill 및 Registry Cutover Runbook](database-backfill-with-replicas.md): Storage usage와 Form/Chat ownership의 backfill, replica 검증, cleanup/enforcement cutover·rollback
- [Certificate PDF Template](domain/certificate-pdf-template.md): 인증서 PDF 배경 템플릿의 품목 개수, 글자 크기, 좌표 조정 방식
- [Community Trophy 제거 릴리스 프리플라이트](domain/community-trophy-removal-release-preflight.md): Trophy 제거 전 운영 승인 게이트와 미실행 항목

## 프로젝트별 운영 문서

- [PLAN_DEVELOPER 3차 종료 후 잔여 TO 자동 배정](project/plan-developer-third-auto-assignment.md)
- [Event Outbox 발행 및 소비 흐름](event-outbox-flow.md): 이벤트 저장, polling, relay, 상태 전이와 dispatch mode 선택 기준
