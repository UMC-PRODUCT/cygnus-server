# Authorization 테스트 케이스

- 테스트 파일: 2개
- 테스트 케이스: 19개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 8 |
| UseCase / Application Service | 11 |

## Controller / Inbound Adapter

### ResourcePermissionControllerTest
- 테스트 설명: ResourcePermissionController
- 위치: `src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [58](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java#L58) | ResourcePermissionController / 특정 리소스와 특정 권한을 지정해 권한을 조회한다 | HTTP GET /api/v1/authorization/resource-permission; param resourceType="NOTICE"; param resourceId=String.valueOf(RESOURCE_ID; param permissionType="READ" | 성공: HTTP 200 OK |
| [92](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java#L92) | ResourcePermissionController / resourceId 없이 특정 권한을 지정해 타입 단위 권한을 조회한다 | HTTP GET /api/v1/authorization/resource-permission; param resourceType="NOTICE"; param permissionType="READ" | 성공: HTTP 200 OK |
| [125](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java#L125) | GET 요청에 resourceType이 없으면 실패한다 | HTTP GET /api/v1/authorization/resource-permission; param resourceId=String.valueOf(RESOURCE_ID; param permissionType="READ" | 실패: HTTP 400 Bad Request |
| [139](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java#L139) | GET 요청에 유효하지 않은 enum 값이 있으면 실패한다 | HTTP GET /api/v1/authorization/resource-permission; param resourceType="NOTICE"; param permissionType="INVALID" | 실패: HTTP 400 Bad Request |
| [153](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java#L153) | 여러 리소스에 대한 권한을 배치로 조회한다 | HTTP POST /api/v1/authorization/resource-permissions/batch | 성공: HTTP 200 OK |
| [197](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java#L197) | 배치 요청에 resourceType 없이 resourceIds만 있으면 실패한다 | HTTP POST /api/v1/authorization/resource-permissions/batch | 실패: HTTP 400 Bad Request |
| [223](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java#L223) | 배치 요청에 resourceIds가 빈 배열이면 실패한다 | HTTP POST /api/v1/authorization/resource-permissions/batch | 실패: HTTP 400 Bad Request |
| [250](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ResourcePermissionControllerTest.java#L250) | 배치 요청에 permissionTypes가 빈 배열이면 실패한다 | HTTP POST /api/v1/authorization/resource-permissions/batch | 실패: HTTP 400 Bad Request |

## UseCase / Application Service

### CheckResourcePermissionServiceTest
- 위치: `src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [65](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L65) | 권한이 있나요? | 호출 hasPermission(MEMBER_ID, resourceType, RESOURCE_ID) | 성공: 검증 assertThat(result.resourceType()).isEqualTo(ResourceType.NOTICE); assertThat(result.resourceId()).isEqualTo(RESOURCE_ID); assertThat(result.permissions()).containsEntry(PermissionType.READ, true); assertThat(result.pe... |
| [100](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L100) | 권한이 있나요? / 모든 권한이 있는 경우 전부 true를 반환한다 | 호출 hasPermission(MEMBER_ID, resourceType, RESOURCE_ID) | 성공: 검증 assertThat(result.permissions().values()).allMatch(hasPermission -> hasPermission); |
| [117](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L117) | resourceId가 null이면 리소스 타입 전체에 대한 권한을 조회한다 | 호출 hasPermission(MEMBER_ID, resourceType, (Long) null) | 성공: 검증 assertThat(result.resourceId()).isNull(); assertThat(result.permissions()).containsEntry(PermissionType.READ, true); |
| [137](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L137) | 리소스 타입이 지원하는 권한 개수만큼 결과를 반환한다 | 호출 hasPermission(MEMBER_ID, resourceType, RESOURCE_ID) | 성공: 검증 assertThat(result.permissions()).hasSize(resourceType.getSupportedPermissions().size()); |
| [155](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L155) | 특정 권한을 지정하면 해당 권한만 평가한다 | 조건 특정 권한을 지정하면 해당 권한만 평가한다 | 성공: 검증 assertThat(result.resourceType()).isEqualTo(ResourceType.NOTICE); assertThat(result.resourceId()).isEqualTo(RESOURCE_ID); assertThat(result.permissions()).containsOnlyKeys(PermissionType.READ); assertThat(result.permi... |
| [184](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L184) | resourceId 없이 특정 권한을 지정하면 타입 단위 권한을 평가한다 | 조건 resourceId 없이 특정 권한을 지정하면 타입 단위 권한을 평가한다 | 성공: 검증 assertThat(result.resourceId()).isNull(); assertThat(result.permissions()).containsOnlyKeys(PermissionType.CHECK); assertThat(result.permissions()).containsEntry(PermissionType.CHECK, false); |
| [210](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L210) | 지원하지 않는 특정 권한을 지정하면 아무 권한도 평가하지 않고 예외가 발생한다 | 조건 지원하지 않는 특정 권한을 지정하면 아무 권한도 평가하지 않고 예외가 발생한다 | 실패: 예외 AuthorizationDomainException; 에러코드 AuthorizationErrorCode.INVALID_INPUT_VALUE; 검증 .isEqualTo(AuthorizationErrorCode.INVALID_INPUT_VALUE); |
| [227](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L227) | Evaluator가 구현되지 않은 리소스 타입이면 예외가 발생한다 | 호출 hasPermission(MEMBER_ID, resourceType, RESOURCE_ID)) | 실패: 예외 AuthorizationDomainException |
| [240](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L240) | 여러 리소스에 대한 특정 권한을 요청 순서대로 조회한다 | 호출 batchHasPermission(MEMBER_ID, queries) | 성공: 검증 assertThat(result).hasSize(3); assertThat(result.get(0).resourceType()).isEqualTo(ResourceType.NOTICE); assertThat(result.get(0).resourceId()).isEqualTo(100L); assertThat(result.get(0).permissions()).containsEntry(Per... |
| [279](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L279) | 권한을 배치로 조회한다 / 지원하지 않는 권한이 하나라도 있으면 아무 권한도 평가하지 않고 예외가 발생한다 | 호출 batchHasPermission(MEMBER_ID, queries)) | 실패: 예외 AuthorizationDomainException; 에러코드 AuthorizationErrorCode.INVALID_INPUT_VALUE; 검증 .isEqualTo(AuthorizationErrorCode.INVALID_INPUT_VALUE); |
| [297](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionServiceTest.java#L297) | resourceIds가 null이면 타입 단위 권한을 배치로 조회한다 | 호출 batchHasPermission(MEMBER_ID, queries) | 성공: 검증 assertThat(result).hasSize(1); assertThat(result.getFirst().resourceType()).isEqualTo(ResourceType.NOTICE); assertThat(result.getFirst().resourceId()).isNull(); assertThat(result.getFirst().permissions()).containsEntr... |
