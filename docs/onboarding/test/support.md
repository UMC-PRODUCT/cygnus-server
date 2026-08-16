# Support 테스트 케이스

- 테스트 파일: 1개
- 테스트 케이스: 1개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Support / Config / Utility | 1 |

## Support / Config / Utility

### CommonFixtureTest
- 위치: `src/test/java/com/umc/product/support/CommonFixtureTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [12](../../../src/test/java/com/umc/product/support/CommonFixtureTest.java#L12) | FailoverIntrospector가 3가지 방식의 객체를 모두 정상적으로 생성한다 | 조건 FailoverIntrospector가 3가지 방식의 객체를 모두 정상적으로 생성한다 | 성공: 검증 assertThat(builderObj).isNotNull(); assertThat(builderObj.getName()).isNotBlank(); // defaultNotNull(true) 덕분에 null이 아님; assertThat(beanObj).isNotNull(); assertThat(beanObj.getTitle()).isNotBlank(); |
