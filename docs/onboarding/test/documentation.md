# Documentation 테스트 케이스

- 테스트 파일: 2개
- 테스트 케이스: 4개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 1 |
| UseCase / Application Service | 3 |

## Controller / Inbound Adapter

### ErrorCodeCatalogControllerTest
- 테스트 설명: ErrorCodeCatalogController
- 위치: `src/test/java/com/umc/product/documentation/adapter/in/web/ErrorCodeCatalogControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [28](../../../src/test/java/com/umc/product/documentation/adapter/in/web/ErrorCodeCatalogControllerTest.java#L28) | GET /api/v1/docs/error-codes 에러 코드 목록을 ApiResponse로 반환한다 | HTTP GET /api/v1/docs/error-codes | 성공: HTTP 200 OK |

## UseCase / Application Service

### ErrorCodeCatalogQueryServiceTest
- 테스트 설명: ErrorCodeCatalogQueryService
- 위치: `src/test/java/com/umc/product/documentation/application/service/ErrorCodeCatalogQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [18](../../../src/test/java/com/umc/product/documentation/application/service/ErrorCodeCatalogQueryServiceTest.java#L18) | 생성된 ErrorCode v1 manifest를 classpath에서 읽는다 | 호출 getErrorCodeCatalog() | 성공: 검증 assertThat(catalog.schemaVersion()).isEqualTo(1); assertThat(catalog.service()).isEqualTo("umc-product-server"); assertThat(catalog.generatedAt()).isNull(); assertThat(catalog.totalCount()).isEqualTo(catalog.items().s... |
| [42](../../../src/test/java/com/umc/product/documentation/application/service/ErrorCodeCatalogQueryServiceTest.java#L42) | ErrorCodeCatalogQueryService / metadata 선언이 없으면 optional field 기본값을 manifest에 반영한다 | 호출 getErrorCodeCatalog() | 성공: 검증 assertThat(item.description()).isNull(); assertThat(item.clientAction()).isNull(); assertThat(item.retryable()).isNull(); assertThat(item.severity()).isNull(); |
| [62](../../../src/test/java/com/umc/product/documentation/application/service/ErrorCodeCatalogQueryServiceTest.java#L62) | ErrorCodeCatalogQueryService / 생성된 ErrorCode manifest를 한 번만 읽고 캐싱한다 | 호출 getErrorCodeCatalog() | 성공: 검증 assertThat(first).isSameAs(response); assertThat(second).isSameAs(response); |
