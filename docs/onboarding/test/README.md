# 테스트 케이스 문서

이 문서는 `src/test/java/com/umc/product` 기준으로 온보딩 문서에 정리된 실행 테스트 케이스를 도메인별로 보여줍니다.
각 도메인 문서는 Controller, UseCase, Repository, E2E 등 큰 카테고리로 먼저 나누고, 각 케이스가 어떤 입력/조건에서 성공 또는 실패를 검증하는지 표로 기록합니다.
표의 수치는 각 도메인 문서가 다루는 범위이며 저장소 전체 JUnit 실행 합계를 뜻하지 않습니다.

| 도메인 | 테스트 파일 수 | 테스트 케이스 수 | 문서 |
|---|---:|---:|---|
| Analytics | 7 | 25 | [analytics.md](analytics.md) |
| Audit | 2 | 5 | [audit.md](audit.md) |
| Authentication | 14 | 71 | [authentication.md](authentication.md) |
| Authorization | 2 | 19 | [authorization.md](authorization.md) |
| Blog | 3 | 33 | [blog.md](blog.md) |
| Challenger | 13 | 56 | [challenger.md](challenger.md) |
| Curriculum | 5 | 61 | [curriculum.md](curriculum.md) |
| Global | 17 | 67 | [global.md](global.md) |
| LLM | 4 | 15 | [llm.md](llm.md) |
| Maintenance | 8 | 47 | [maintenance.md](maintenance.md) |
| Member | 20 | 100 | [member.md](member.md) |
| Notification | 3 | 10 | [notification.md](notification.md) |
| Organization | 32 | 168 | [organization.md](organization.md) |
| Project | 37 | 526 | [project.md](project.md) |
| Recruiting | 78 | 346 | [recruiting.md](recruiting.md) |
| Schedule | 2 | 16 | [schedule.md](schedule.md) |
| Storage | 5 | 40 | [storage.md](storage.md) |
| Support | 1 | 1 | [support.md](support.md) |
| Term | 8 | 23 | [term.md](term.md) |
| Test Seed | 11 | 58 | [test.md](test.md) |

- 문서화된 20개 도메인 행 합계: 테스트 파일 272개, 테스트 케이스 1,687개
- 최신 전체 실행 합계: Task 12 격리 JUnit XML snapshot 기준 446 suites, 2,172 tests, failures 0, errors 0, skipped 40

## 참고: 실행 테스트 메서드가 없는 지원 파일

- `src/test/java/com/umc/product/organization/application/port/in/command/ManageStudyGroupUseCaseTest.java`
- `src/test/java/com/umc/product/support/ControllerTestSupport.java`
- `src/test/java/com/umc/product/support/PersistenceAdapterTest.java`
