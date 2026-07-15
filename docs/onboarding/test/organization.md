# Organization 테스트 케이스

- 테스트 파일: 34개
- 테스트 케이스: 193개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 24 |
| UseCase / Application Service | 100 |
| Repository / Outbound Persistence | 33 |
| Domain | 36 |

## Controller / Inbound Adapter

### AdminSchoolQueryControllerTest
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/AdminSchoolQueryControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [23](../../../src/test/java/com/umc/product/organization/adapter/in/web/AdminSchoolQueryControllerTest.java#L23) | 학교 상세정보를 조회합니다 | HTTP GET /api/v1/schools/{schoolId} | 성공: HTTP 200 OK |
| [68](../../../src/test/java/com/umc/product/organization/adapter/in/web/AdminSchoolQueryControllerTest.java#L68) | 학교 전체 목록을 조회합니다 | HTTP GET /api/v1/schools/all | 성공: HTTP 200 OK |

### ChapterCommandControllerTest
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/ChapterCommandControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [16](../../../src/test/java/com/umc/product/organization/adapter/in/web/ChapterCommandControllerTest.java#L16) | 신규 지부를 생성한다 | HTTP POST /api/v1/chapters | 성공: HTTP 200 OK |

### ChapterQueryControllerTest
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/ChapterQueryControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [16](../../../src/test/java/com/umc/product/organization/adapter/in/web/ChapterQueryControllerTest.java#L16) | 지부 목록을 조회합니다 | HTTP GET /api/v1/chapters | 성공: HTTP 200 OK |

### UmcProductRequestContractTest
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/UmcProductRequestContractTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [41](../../../src/test/java/com/umc/product/organization/adapter/in/web/UmcProductRequestContractTest.java#L41) | 활동 날짜는 yyyy-MM-dd 형식으로 역직렬화한다 | 날짜만 포함한 활동 기간 JSON | 성공: `LocalDate`로 같은 날짜를 반환한다 |
| [62](../../../src/test/java/com/umc/product/organization/adapter/in/web/UmcProductRequestContractTest.java#L62) | 활동 날짜에 시각이나 offset이 포함되면 역직렬화를 거부한다 | `2026-07-13T00:00:00Z` 입력 | 실패: `LocalDate` 형식 오류 |
| [78](../../../src/test/java/com/umc/product/organization/adapter/in/web/UmcProductRequestContractTest.java#L78) | activeOn은 yyyy-MM-dd 형식만 허용한다 | offset 또는 Z가 포함된 query parameter | 실패: 날짜 변환 오류 |
| [101](../../../src/test/java/com/umc/product/organization/adapter/in/web/UmcProductRequestContractTest.java#L101) | 멤버 생성에는 한 개 이상의 활동 기간이 필요하다 | 빈 `activityPeriods` | 실패: Jakarta Validation 위반 |
| [135](../../../src/test/java/com/umc/product/organization/adapter/in/web/UmcProductRequestContractTest.java#L135) | Chapter 소속 요청에는 Part와 역할 필드가 없다 | Chapter membership record component 조회 | 성공: `chapterId`는 있고 `partId`, `role`은 없다 |

### GisuCommandControllerTest
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/GisuCommandControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [24](../../../src/test/java/com/umc/product/organization/adapter/in/web/GisuCommandControllerTest.java#L24) | 신규 기수를 추가한다 | HTTP POST /api/v1/gisu | 성공: HTTP 200 OK |
| [45](../../../src/test/java/com/umc/product/organization/adapter/in/web/GisuCommandControllerTest.java#L45) | 기수를 삭제한다 | HTTP DELETE /api/v1/gisu/{gisuId} | 성공: HTTP 200 OK |
| [58](../../../src/test/java/com/umc/product/organization/adapter/in/web/GisuCommandControllerTest.java#L58) | 현재 기수를 설정한다 | HTTP POST /api/v1/gisu/{gisuId}/active | 성공: HTTP 200 OK |

### GisuQueryControllerTest
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/GisuQueryControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [24](../../../src/test/java/com/umc/product/organization/adapter/in/web/GisuQueryControllerTest.java#L24) | 기수 목록을 페이징 조회한다 | HTTP GET /api/v1/gisu; param page=String.valueOf(page; param size=String.valueOf(size | 성공: HTTP 200 OK |
| [78](../../../src/test/java/com/umc/product/organization/adapter/in/web/GisuQueryControllerTest.java#L78) | 기수 전체 목록을 조회한다 | HTTP GET /api/v1/gisu/all | 성공: HTTP 200 OK |
| [108](../../../src/test/java/com/umc/product/organization/adapter/in/web/GisuQueryControllerTest.java#L108) | 활성화된 기수를 조회한다 | HTTP GET /api/v1/gisu/active | 성공: HTTP 200 OK |

### GisuQueryV2ControllerTest
- 테스트 설명: GisuQueryV2Controller
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/v2/GisuQueryV2ControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [45](../../../src/test/java/com/umc/product/organization/adapter/in/web/v2/GisuQueryV2ControllerTest.java#L45) | id 목록은 중복을 제거하고 첫 등장 순서대로 조회한다 | HTTP GET /api/v2/gisu; param id="1", "1", "2"; param includeChapter="true"; param includeSchool="true" | 성공: HTTP 200 OK |
| [133](../../../src/test/java/com/umc/product/organization/adapter/in/web/v2/GisuQueryV2ControllerTest.java#L133) | generation 목록은 중복을 제거하고 조회한다 | HTTP GET /api/v2/gisu; param generation="9", "9", "10" | 성공: HTTP 200 OK |
| [154](../../../src/test/java/com/umc/product/organization/adapter/in/web/v2/GisuQueryV2ControllerTest.java#L154) | active=true는 활성 기수만 조회한다 | HTTP GET /api/v2/gisu; param active="true" | 성공: HTTP 200 OK |
| [170](../../../src/test/java/com/umc/product/organization/adapter/in/web/v2/GisuQueryV2ControllerTest.java#L170) | 조회 기준이 없으면 400을 반환한다 | HTTP GET /api/v2/gisu | 실패: HTTP 400 Bad Request |
| [180](../../../src/test/java/com/umc/product/organization/adapter/in/web/v2/GisuQueryV2ControllerTest.java#L180) | 조회 기준을 둘 이상 보내면 400을 반환한다 | HTTP GET /api/v2/gisu; param id="1"; param generation="9" | 실패: HTTP 400 Bad Request |
| [192](../../../src/test/java/com/umc/product/organization/adapter/in/web/v2/GisuQueryV2ControllerTest.java#L192) | active=false는 400을 반환한다 | HTTP GET /api/v2/gisu; param active="false" | 실패: HTTP 400 Bad Request |

### SchoolCommandControllerTest
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/SchoolCommandControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [23](../../../src/test/java/com/umc/product/organization/adapter/in/web/SchoolCommandControllerTest.java#L23) | 총괄 신규학교를 추가한다 | HTTP POST /api/v1/schools | 성공: HTTP 200 OK |
| [52](../../../src/test/java/com/umc/product/organization/adapter/in/web/SchoolCommandControllerTest.java#L52) | 총괄 학교정보를 수정한다 | HTTP PATCH /api/v1/schools/{schoolId} | 성공: HTTP 200 OK |
| [79](../../../src/test/java/com/umc/product/organization/adapter/in/web/SchoolCommandControllerTest.java#L79) | 총괄 학교를 일괄 삭제한다 | HTTP DELETE /api/v1/schools | 성공: HTTP 200 OK |

### SchoolLinkQueryControllerTest
- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/SchoolLinkQueryControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [19](../../../src/test/java/com/umc/product/organization/adapter/in/web/SchoolLinkQueryControllerTest.java#L19) | 학교 링크를 조회합니다 | HTTP GET /api/v1/schools/link/{schoolId} | 성공: HTTP 200 OK |

## UseCase / Application Service

### ChapterQueryServiceTest
- 테스트 설명: ChapterQueryService
- 위치: `src/test/java/com/umc/product/organization/application/port/service/query/ChapterQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [29](../../../src/test/java/com/umc/product/organization/application/port/service/query/ChapterQueryServiceTest.java#L29) | listByGisuIds는 기수별 지부 목록을 그룹화한다 | 조건 listByGisuIds는 기수별 지부 목록을 그룹화한다 | 성공: 검증 assertThat(result.get(1L)).extracting(ChapterInfo::name).containsExactly("Scorpio"); assertThat(result.get(2L)).extracting(ChapterInfo::name).containsExactly("Ain"); |
| [58](../../../src/test/java/com/umc/product/organization/application/port/service/query/ChapterQueryServiceTest.java#L58) | ChapterQueryService / getChaptersWithSchoolsByGisuIds는 기수별 지부와 학교 목록을 그룹화한다 | 호출 getChaptersWithSchoolsByGisuIds(gisuIds) | 성공: 검증 assertThat(result.get(1L).getFirst().schools()); .containsExactly("A 대학교"); assertThat(result.get(2L).getFirst().schools()); .containsExactly("B 대학교"); |

### GetChapterUseCaseTest
- 위치: `src/test/java/com/umc/product/organization/application/port/in/query/GetChapterUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [37](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetChapterUseCaseTest.java#L37) | 전체 지부 목록을 조회한다 | 조건 전체 지부 목록을 조회한다 | 성공: 검증 assertThat(result).hasSize(3).extracting(ChapterInfo::name); .containsExactlyInAnyOrder("서울", "경기", "인천"); |
| [54](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetChapterUseCaseTest.java#L54) | 기수별 지부와 소속 학교 목록을 조회한다 | 조건 기수별 지부와 소속 학교 목록을 조회한다 | 성공: 검증 assertThat(result).hasSize(2); assertThat(scorpioResult.schools()).hasSize(2); .containsExactlyInAnyOrder("한성대", "동국대"); assertThat(leoResult.schools()).hasSize(1) |
| [93](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetChapterUseCaseTest.java#L93) | 학교가 없는 지부도 조회된다 | 조건 학교가 없는 지부도 조회된다 | 성공: 검증 assertThat(result).hasSize(2); assertThat(result).allMatch(c -> c.schools().isEmpty()); |

### GetGisuUseCaseTest
- 위치: `src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [28](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L28) | 전체 기수 목록을 조회한다 | 조건 전체 기수 목록을 조회한다 | 성공: 검증 assertThat(result).hasSize(3); |
| [42](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L42) | 활성 기수와 비활성 기수를 구분할 수 있다 | 조건 활성 기수와 비활성 기수를 구분할 수 있다 | 성공: 검증 assertThat(activeInfo.isActive()).isTrue(); assertThat(inactiveInfo.isActive()).isFalse(); |
| [57](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L57) | 기수 정보에 시작일과 종료일이 포함된다 | 조건 기수 정보에 시작일과 종료일이 포함된다 | 성공: 검증 assertThat(result.startAt()).isEqualTo(startAt); assertThat(result.endAt()).isEqualTo(endAt); |
| [72](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L72) | 기수 목록을 페이징하여 조회한다 | 조건 기수 목록을 페이징하여 조회한다 | 성공: 검증 assertThat(result.getContent()).hasSize(3); assertThat(result.getTotalElements()).isEqualTo(5); assertThat(result.getTotalPages()).isEqualTo(2); assertThat(result.getNumber()).isEqualTo(0); |
| [93](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L93) | 기수 페이징 마지막 페이지를 조회한다 | 조건 기수 페이징 마지막 페이지를 조회한다 | 성공: 검증 assertThat(result.getContent()).hasSize(2); assertThat(result.getTotalElements()).isEqualTo(5); assertThat(result.getNumber()).isEqualTo(1); assertThat(result.hasNext()).isFalse(); |
| [113](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L113) | 기수가 없으면 빈 페이지를 반환한다 | 조건 기수가 없으면 빈 페이지를 반환한다 | 성공: 검증 assertThat(result.getContent()).isEmpty(); assertThat(result.getTotalElements()).isEqualTo(0); assertThat(result.getTotalPages()).isEqualTo(0); assertThat(result.hasNext()).isFalse(); |
| [128](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L128) | 페이징 결과가 generation 내림차순으로 정렬된다 | 조건 페이징 결과가 generation 내림차순으로 정렬된다 | 성공: 검증 assertThat(result.getContent()).hasSize(3); assertThat(result.getContent().get(0).generation()).isEqualTo(9L); assertThat(result.getContent().get(1).generation()).isEqualTo(8L); assertThat(result.getContent().get(2).g... |
| [147](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L147) | 전체 기수 이름 목록을 조회한다 | 조건 전체 기수 이름 목록을 조회한다 | 성공: 검증 assertThat(result).hasSize(3); assertThat(result).extracting(GisuNameInfo::generation); .containsExactly(9L, 8L, 7L); |
| [163](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L163) | 기수가 없으면 빈 이름 목록을 반환한다 | 조건 기수가 없으면 빈 이름 목록을 반환한다 | 성공: 검증 assertThat(result).isEmpty(); |
| [172](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L172) | 활성화된 기수를 조회한다 | 조건 활성화된 기수를 조회한다 | 성공: 검증 assertThat(result.gisuId()).isEqualTo(activeGisu.getId()); assertThat(result.generation()).isEqualTo(8L); assertThat(result.isActive()).isTrue(); |
| [187](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetGisuUseCaseTest.java#L187) | 활성화된 기수가 없으면 예외가 발생한다 | 조건 활성화된 기수가 없으면 예외가 발생한다 | 실패: 예외 com.umc.product.global.exception.BusinessException |

### GetSchoolUseCaseTest
- 위치: `src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [47](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java#L47) | 배정 대기 중인 학교 목록을 조회한다 | 조건 배정 대기 중인 학교 목록을 조회한다 | 성공: 검증 assertThat(result).hasSize(2); .containsExactlyInAnyOrder("동국대", "중앙대"); |
| [68](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java#L68) | 다른 기수에 배정된 학교는 배정 대기로 조회된다 | 조건 다른 기수에 배정된 학교는 배정 대기로 조회된다 | 성공: 검증 assertThat(result).hasSize(1); .containsExactly("동국대"); |
| [92](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java#L92) | 모든 학교가 배정되어 있으면 빈 목록을 반환한다 | 조건 모든 학교가 배정되어 있으면 빈 목록을 반환한다 | 성공: 검증 assertThat(result).isEmpty(); |
| [108](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java#L108) | 학교 상세를 조회한다 활성 기수 지부 정보를 포함한다 | 조건 학교 상세를 조회한다 활성 기수 지부 정보를 포함한다 | 성공: 검증 assertThat(result.schoolId()).isEqualTo(school.getId()); assertThat(result.schoolName()).isEqualTo("중앙대"); assertThat(result.remark()).isEqualTo("비고"); assertThat(result.chapterId()).isEqualTo(chapter.getId()); |
| [131](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java#L131) | 비활성 기수 지부는 상세 조회에서 null로 반환된다 | 조건 비활성 기수 지부는 상세 조회에서 null로 반환된다 | 성공: 검증 assertThat(result.schoolId()).isEqualTo(school.getId()); assertThat(result.chapterId()).isNull(); assertThat(result.chapterName()).isNull(); |
| [150](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java#L150) | 전체 학교 이름 목록을 조회한다 | 조건 전체 학교 이름 목록을 조회한다 | 성공: 검증 assertThat(result).hasSize(3); assertThat(result).extracting(SchoolNameInfo::schoolName); .containsExactly("동국대", "중앙대", "한성대"); |
| [166](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java#L166) | 학교가 없으면 빈 목록을 반환한다 | 조건 학교가 없으면 빈 목록을 반환한다 | 성공: 검증 assertThat(result).isEmpty(); |
| [175](../../../src/test/java/com/umc/product/organization/application/port/in/query/GetSchoolUseCaseTest.java#L175) | 존재하지 않는 학교 상세를 조회하면 예외가 발생한다 | 조건 존재하지 않는 학교 상세를 조회하면 예외가 발생한다 | 실패: 예외 BusinessException |

### GisuOrganizationQueryServiceTest
- 테스트 설명: GisuOrganizationQueryService
- 위치: `src/test/java/com/umc/product/organization/application/port/service/query/GisuOrganizationQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [32](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuOrganizationQueryServiceTest.java#L32) | id 조회는 기수만 반환하고 include가 false면 지부와 학교를 조회하지 않는다 | 호출 get(GisuOrganizationQuery.byIds(List.of(1L, 2L), false, false)) | 성공: 검증 assertThat(result).extracting(GisuOrganizationInfo::gisuId).containsExactly(1L, 2L); assertThat(result.getFirst().chapters()).isEmpty(); assertThat(result.getFirst().schools()).isEmpty(); |
| [62](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuOrganizationQueryServiceTest.java#L62) | GisuOrganizationQueryService / generation 조회는 지부와 학교를 모두 포함해서 조립한다 | 조건 GisuOrganizationQueryService / generation 조회는 지부와 학교를 모두 포함해서 조립한다 | 성공: 검증 assertThat(info.gisuId()).isEqualTo(2L); assertThat(info.chapters()).hasSize(1); assertThat(info.chapters().getFirst().schools()).extracting(GisuOrganizationInfo.ChapterSchoolInfo::schoolId); .containsExactly(101L); |
| [88](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuOrganizationQueryServiceTest.java#L88) | 지부만 포함하면 지부 내 학교 목록은 비워서 반환한다 | 호출 get(GisuOrganizationQuery.byIds(List.of(2L), true, false)) | 성공: 검증 assertThat(result.getFirst().chapters()).hasSize(1); assertThat(result.getFirst().chapters().getFirst().schools()).isEmpty(); assertThat(result.getFirst().schools()).isEmpty(); |
| [106](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuOrganizationQueryServiceTest.java#L106) | 여러 기수 조회는 지부와 학교를 기수 목록 단위로 한 번에 조회한다 | 호출 get(GisuOrganizationQuery.byIds(List.of(1L, 2L), true, true)) | 성공: 검증 assertThat(result).extracting(GisuOrganizationInfo::gisuId).containsExactly(1L, 2L); assertThat(result.get(0).chapters()).extracting(GisuOrganizationInfo.ChapterOrganizationInfo::chapterId); .containsExactly(10L); asse... |
| [137](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuOrganizationQueryServiceTest.java#L137) | active 조회는 활성 기수 하나를 반환한다 | 호출 get(GisuOrganizationQuery.active(false, false)) | 성공: 검증 assertThat(result).extracting(GisuOrganizationInfo::gisuId).containsExactly(2L); |

### GisuQueryServiceTest
- 테스트 설명: GisuQueryService
- 위치: `src/test/java/com/umc/product/organization/application/port/service/query/GisuQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [26](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuQueryServiceTest.java#L26) | batchGetByIds는 중복을 제거하고 요청 순서를 보존한다 | 호출 batchGetByIds(Arrays.asList(2L, null, 1L, 2L)) | 실패: 검증 assertThat(result).extracting(GisuInfo::gisuId).containsExactly(2L, 1L); |
| [47](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuQueryServiceTest.java#L47) | GisuQueryService / batchGetByGenerations는 중복을 제거하고 요청 순서를 보존한다 | 호출 batchGetByGenerations(List.of(10L, 9L, 10L)) | 실패: 검증 assertThat(result).extracting(GisuInfo::generation).containsExactly(10L, 9L); |
| [60](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuQueryServiceTest.java#L60) | GisuQueryService / batchGetByIds는 요청한 기수가 하나라도 없으면 GISU_NOT_FOUND를 던진다 | 호출 batchGetByIds(List.of(1L, 999L))) | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.GISU_NOT_FOUND |
| [72](../../../src/test/java/com/umc/product/organization/application/port/service/query/GisuQueryServiceTest.java#L72) | GisuQueryService / batchGetByGenerations는 요청한 기수가 하나라도 없으면 GISU_NOT_FOUND를 던진다 | 호출 batchGetByGenerations(List.of(9L, 999L))) | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.GISU_NOT_FOUND |

### ManageChapterUseCaseTest
- 위치: `src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [39](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L39) | 지부를 생성한다 | CreateChapterCommand(gisu.getId(), "Scorpio", List.of()); 호출 create(command) | 성공: 검증 assertThat(chapter.getName()).isEqualTo("Scorpio"); assertThat(chapter.getGisu().getId()).isEqualTo(gisu.getId()); |
| [54](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L54) | 학교와 함께 지부를 생성한다 | CreateChapterCommand(gisu.getId(), "Scorpio", List.of(school1.getId(), school2.getId())); 호출 create(command) | 성공: 검증 assertThat(chapter.getName()).isEqualTo("Scorpio"); assertThat(savedSchool1.getChapterSchools()).hasSize(1); assertThat(savedSchool1.getChapterSchools().get(0).getChapter().getId()).isEqualTo(chapterId); assertThat(sa... |
| [84](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L84) | 존재하지 않는 학교로 지부를 생성하면 예외가 발생한다 | CreateChapterCommand(gisu.getId(), "서울", List.of(999L)); 호출 create(command)) | 실패: 예외 BusinessException |
| [99](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L99) | 존재하지 않는 기수로 지부를 생성하면 예외가 발생한다 | CreateChapterCommand(999L, "Scorpio", List.of()); 호출 create(command)) | 실패: 예외 BusinessException |
| [109](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L109) | 지부를 삭제한다 | 호출 delete(chapterId) | 실패: 예외 BusinessException |
| [125](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L125) | 지부 삭제 시 소속 학교는 유지된다 | 호출 delete(chapterId) | 성공: 검증 assertThat(loadSchoolPort.existsById(school1.getId())).isTrue(); assertThat(loadSchoolPort.existsById(school2.getId())).isTrue(); |
| [144](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L144) | 지부 삭제 시 학교와 지부 연결이 제거된다 | 호출 delete(chapterId) | 성공: 검증 assertThat(loadChapterSchoolPort.findByGisuId(gisu.getId())).isEmpty(); |
| [161](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L161) | 존재하지 않는 지부를 삭제하면 예외가 발생한다 | 호출 delete(999L)) | 실패: 예외 BusinessException |
| [168](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L168) | 같은 기수에 중복된 지부 이름으로 생성하면 예외가 발생한다 | 호출 create(new CreateChapterCommand(gisu.getId(), "Scorpio", List.of())) | 실패: 예외 BusinessException |
| [180](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageChapterUseCaseTest.java#L180) | 같은 기수에 이미 다른 지부에 배정된 학교로 생성하면 예외가 발생한다 | 조건 같은 기수에 이미 다른 지부에 배정된 학교로 생성하면 예외가 발생한다 | 실패: 예외 BusinessException |

### ManageGisuUseCaseTest
- 위치: `src/test/java/com/umc/product/organization/application/port/in/command/ManageGisuUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [26](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageGisuUseCaseTest.java#L26) | 신규 기수를 생성한다 | CreateGisuCommand(10L, Instant.parse("2025-03-01T00:00:00Z"), Instant.parse("2025-08-31T23:59:59Z")); 호출 create(command) | 성공: 검증 assertThat(gisuId).isNotNull(); assertThat(savedGisu.generation()).isEqualTo(10L); assertThat(savedGisu.isActive()).isFalse(); |
| [45](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageGisuUseCaseTest.java#L45) | 이미 존재하는 기수 번호로 생성하면 예외가 발생한다 | CreateGisuCommand(10L, Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-08-31T23:59:59Z")); 호출 create(command)) | 실패: 예외 BusinessException; 에러코드 OrganizationErrorCode.GISU_ALREADY_EXISTS; 검증 .isEqualTo(OrganizationErrorCode.GISU_ALREADY_EXISTS); |
| [63](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageGisuUseCaseTest.java#L63) | 기수를 삭제한다 | 호출 deleteGisu(gisuId) | 실패: 예외 BusinessException; 에러코드 OrganizationErrorCode.GISU_NOT_FOUND; 검증 .isEqualTo(OrganizationErrorCode.GISU_NOT_FOUND); |
| [78](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageGisuUseCaseTest.java#L78) | 존재하지 않는 기수를 삭제하면 예외가 발생한다 | 호출 deleteGisu(nonExistentGisuId)) | 실패: 예외 BusinessException; 에러코드 OrganizationErrorCode.GISU_NOT_FOUND; 검증 .isEqualTo(OrganizationErrorCode.GISU_NOT_FOUND); |
| [90](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageGisuUseCaseTest.java#L90) | 활성 기수를 변경한다 | 호출 updateActiveGisu(newGisuId) | 성공: 검증 assertThat(oldGisuInfo.isActive()).isFalse(); assertThat(newGisuInfo.isActive()).isTrue(); |

### ManageSchoolUseCaseTest
- 위치: `src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [45](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L45) | 학교를 등록한다 | CreateSchoolCommand("한성대", "비고", null, List.of()); 호출 create(command) | 성공: 검증 assertThat(savedSchool.getName()).isEqualTo("한성대"); assertThat(savedSchool.getRemark()).isEqualTo("비고"); |
| [59](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L59) | 외부링크와 함께 학교를 등록한다 | CreateSchoolCommand("한성대", "비고", null, links); 호출 create(command) | 성공: 검증 assertThat(savedSchool.getName()).isEqualTo("한성대"); assertThat(savedLinks).hasSize(3); assertThat(savedLinks); .containsExactlyInAnyOrder(SchoolLinkType.KAKAO, SchoolLinkType.INSTAGRAM, SchoolLinkType.YOUTUBE); |
| [83](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L83) | 같은 타입의 링크를 여러개 등록할 수 있다 | CreateSchoolCommand("한성대", "비고", null, links); 호출 create(command) | 성공: 검증 assertThat(savedLinks).hasSize(2); assertThat(savedLinks); .containsOnly(SchoolLinkType.INSTAGRAM); assertThat(savedLinks) |
| [106](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L106) | 같은 타입의 링크가 등록된 상태에서 수정할 수 있다 | CreateSchoolCommand("한성대", "비고", null, initialLinks); UpdateSchoolCommand("한성대", null, "비고", null, updatedLinks); 호출 create(createCommand); 호출 updateSchool(schoolId, updateCommand) | 성공: 검증 assertThat(savedUpdatedLinks).hasSize(3); assertThat(savedUpdatedLinks); .containsExactlyInAnyOrder(SchoolLinkType.INSTAGRAM, SchoolLinkType.INSTAGRAM, SchoolLinkType.KAKAO); assertThat(savedUpdatedLinks) |
| [140](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L140) | 학교 이름과 비고를 수정한다 | UpdateSchoolCommand("동국대", null, "수정된 비고", null, null); 호출 updateSchool(school.getId(), command) | 성공: 검증 assertThat(updatedSchool.getName()).isEqualTo("동국대"); assertThat(updatedSchool.getRemark()).isEqualTo("수정된 비고"); |
| [156](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L156) | 학교 수정 시 링크도 함께 수정한다 | UpdateSchoolCommand("한성대", null, "비고", null, updatedLinks); 호출 updateSchool(school.getId(); 호출 updateSchool(school.getId(), command) | 성공: 검증 assertThat(savedUpdatedLinks).hasSize(2); assertThat(savedUpdatedLinks); .containsExactlyInAnyOrder(SchoolLinkType.INSTAGRAM, SchoolLinkType.YOUTUBE); assertThat(savedUpdatedLinks) |
| [186](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L186) | 학교의 지부를 수정한다 | UpdateSchoolCommand("한성대", leoChapter.getId(), "비고", null, null); 호출 updateSchool(school.getId(), command) | 성공: 검증 assertThat(updatedSchool.getChapterSchools()).hasSize(1); assertThat(updatedSchool.getChapterSchools().get(0).getChapter().getName()).isEqualTo("Leo"); |
| [206](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L206) | 존재하지 않는 학교를 수정하면 예외가 발생한다 | UpdateSchoolCommand("동국대", null, "비고", null, null); 호출 updateSchool(999L, command)) | 실패: 예외 BusinessException |
| [216](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L216) | 학교를 삭제한다 | 호출 deleteSchools(List.of(school1.getId())) | 실패: 예외 BusinessException; 검증 assertThat(remainingSchool.getName()).isEqualTo("동국대"); |
| [233](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L233) | 여러 학교를 한번에 삭제한다 | 호출 deleteSchools(List.of(school1.getId(), school2.getId())) | 실패: 예외 BusinessException; 검증 assertThat(remainingSchool.getName()).isEqualTo("중앙대"); |
| [253](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L253) | 학교를 지부에 배정한다 | AssignSchoolCommand(school.getId(), chapter.getId()); 호출 assignToChapter(command) | 성공: 검증 assertThat(updatedSchool.getChapterSchools()).hasSize(1); assertThat(updatedSchool.getChapterSchools().get(0).getChapter().getName()).isEqualTo("Scorpio"); |
| [271](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L271) | 이미 배정된 학교를 다른 지부로 이동한다 | AssignSchoolCommand(school.getId(), leoChapter.getId()); 호출 assignToChapter(command) | 성공: 검증 assertThat(updatedSchool.getChapterSchools()).hasSize(1); assertThat(updatedSchool.getChapterSchools().get(0).getChapter().getName()).isEqualTo("Leo"); |
| [292](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L292) | 다른 기수 배정은 유지하면서 특정 기수 지부로 배정한다 | AssignSchoolCommand(school.getId(), chapter9.getId()); 호출 assignToChapter(command) | 성공: 검증 assertThat(updatedSchool.getChapterSchools()).hasSize(2); assertThat(updatedSchool.getChapterSchools()); .containsExactlyInAnyOrder("Scorpio", "Leo"); |
| [317](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L317) | 학교의 지부 배정을 해제한다 | UnassignSchoolCommand(school.getId(), gisu.getId()); 호출 unassignFromChapter(command) | 성공: 검증 assertThat(updatedSchool.getChapterSchools()).isEmpty(); |
| [336](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L336) | 특정 기수의 배정만 해제하고 다른 기수는 유지한다 | UnassignSchoolCommand(school.getId(), gisu9.getId()); 호출 unassignFromChapter(command) | 성공: 검증 assertThat(updatedSchool.getChapterSchools()).hasSize(1); assertThat(updatedSchool.getChapterSchools().get(0).getChapter().getName()).isEqualTo("Leo"); |
| [360](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L360) | 존재하지 않는 학교를 배정하면 예외가 발생한다 | AssignSchoolCommand(999L, chapter.getId()); 호출 assignToChapter(command)) | 실패: 예외 BusinessException |
| [373](../../../src/test/java/com/umc/product/organization/application/port/in/command/ManageSchoolUseCaseTest.java#L373) | 존재하지 않는 지부에 배정하면 예외가 발생한다 | AssignSchoolCommand(school.getId(), 999L); 호출 assignToChapter(command)) | 실패: 예외 BusinessException |

### SchoolQueryServiceTest
- 테스트 설명: SchoolQueryService
- 위치: `src/test/java/com/umc/product/organization/application/port/service/query/SchoolQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [25](../../../src/test/java/com/umc/product/organization/application/port/service/query/SchoolQueryServiceTest.java#L25) | getSchoolListByGisuIds는 기수별 학교 상세 목록을 그룹화한다 | 호출 getSchoolListByGisuIds(gisuIds) | 성공: 검증 assertThat(result.get(1L)).extracting(SchoolDetailInfo::schoolName).containsExactly("A 대학교"); assertThat(result.get(1L).getFirst().logoImageUrl()).isEqualTo("https://cdn.example.com/logo-a.png"); assertThat(result.get(... |
| [66](../../../src/test/java/com/umc/product/organization/application/port/service/query/SchoolQueryServiceTest.java#L66) | SchoolQueryService / getSchoolListByGisuIds는 기수 목록이 비어 있으면 조회하지 않는다 | 호출 getSchoolListByGisuIds(new LinkedHashSet<>()) | 성공: 검증 assertThat(result).isEmpty(); |

### StudyGroupQueryServiceTest
- 위치: `src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [57](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L57) | getMyStudyGroups 회장은 AsSchoolCore scope로 조회 | 호출 getMyStudyGroups(memberId, null, 20) | 성공: 검증 assertThat(capturedScopes).hasSize(1); assertThat(capturedScopes.get(0)).isInstanceOfSatisfying(AsSchoolCore.class, |
| [84](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L84) | getMyStudyGroups 파트장은 AsPartLeader scope로 조회 | 호출 getMyStudyGroups(memberId, null, 20) | 성공: 검증 assertThat(capturedScopes).hasSize(1); assertThat(capturedScopes.get(0)).isInstanceOfSatisfying(AsPartLeader.class, |
| [110](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L110) | getMyStudyGroups 회장과 파트장 겸직시 두 scope 조립 | 호출 getMyStudyGroups(memberId, null, 20) | 성공: 검증 assertThat(capturedScopes).hasSize(2); assertThat(capturedScopes).hasAtLeastOneElementOfType(AsSchoolCore.class); assertThat(capturedScopes).hasAtLeastOneElementOfType(AsPartLeader.class); |
| [138](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L138) | getMyStudyGroups 권한이 없으면 port 호출없이 빈 리스트 | 호출 getMyStudyGroups(memberId, null, 20) | 성공: 검증 assertThat(result).isEmpty(); |
| [159](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L159) | getMyStudyGroups 회장이지만 학교 멤버가 없으면 AsSchoolCore scope 제외 | 호출 getMyStudyGroups(memberId, null, 20) | 실패: 예외 AsPartLeader; 검증 assertThat(capturedScopes).hasSize(1); assertThat(capturedScopes.get(0)).isInstanceOf(AsPartLeader.class); |
| [185](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L185) | getMyStudyGroups 컨트롤러 hasNext 판단을 위해 size plus 1로 port에 전달 | 호출 getMyStudyGroups(memberId, null, requestedSize) | 성공: getMyStudyGroups 컨트롤러 hasNext 판단을 위해 size plus 1로 port에 전달 |
| [208](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L208) | getById StudyGroupInfo에 기본 정보와 멘토 멤버 ID를 담는다 | 호출 getById(groupId) | 성공: 검증 assertThat(result.groupId()).isEqualTo(groupId); assertThat(result.name()).isEqualTo("스프링 스터디"); assertThat(result.mentorIds()).containsExactly(mentorId); assertThat(result.memberIds()).containsExactly(memberId); |
| [231](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L231) | getWithMemberAndMentorInfoById 헤더와 멘토 멤버를 조립한다 | 호출 getWithMemberAndMentorInfoById(groupId) | 성공: 검증 assertThat(result.groupId()).isEqualTo(groupId); assertThat(result.name()).isEqualTo("스프링 스터디"); assertThat(result.mentors()); .containsExactly(mentor1, mentor2); |
| [273](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L273) | getWithMemberAndMentorInfoById Member 조회 누락분은 결과에서 제외된다 | 호출 getWithMemberAndMentorInfoById(groupId) | 성공: 검증 assertThat(result.members()); .containsExactly(existing) |
| [298](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L298) | getWithMemberAndMentorInfoById mentor와 member 둘다 없으면 findAllByIds 호출없이 빈 리스트 | 호출 getWithMemberAndMentorInfoById(groupId) | 성공: 검증 assertThat(result.mentors()).isEmpty(); assertThat(result.members()).isEmpty(); |
| [317](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L317) | findById 존재하면 StudyGroupInfo를 반환한다 | 호출 findById(groupId) | 성공: 검증 assertThat(result).isPresent(); assertThat(result.get().mentorIds()).containsExactly(mentorId); assertThat(result.get().memberIds()).containsExactly(memberId); |
| [339](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L339) | findById 존재하지 않으면 Optional empty를 반환한다 | 호출 findById(groupId) | 성공: 검증 assertThat(result).isEmpty(); |
| [352](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L352) | getStudyGroupMembers 그룹 멤버를 MemberInfo로 조립한다 | 호출 getStudyGroupMembers(groupId) | 성공: 검증 assertThat(result); .containsExactly(member1, member2); assertThat(result).allSatisfy(m -> {; assertThat(m.studyGroupId()).isEqualTo(groupId); |
| [384](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L384) | getStudyGroupMembers 멤버가 없으면 findAllByIds 호출없이 빈 리스트 | 호출 getStudyGroupMembers(groupId) | 성공: 검증 assertThat(result).isEmpty(); |
| [402](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L402) | getStudyGroupMembers Member 조회 누락분은 결과에서 제외된다 | 호출 getStudyGroupMembers(groupId) | 성공: 검증 assertThat(result); .containsExactly(existing) |
| [427](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L427) | resolveOrganizationRoleScopes 회장과 파트장 겸직시 두 scope 반환 | 호출 resolveOrganizationRoleScopes(memberId) | 성공: 검증 assertThat(scopes).hasSize(2); assertThat(scopes).hasAtLeastOneElementOfType(AsSchoolCore.class); assertThat(scopes).hasAtLeastOneElementOfType(AsPartLeader.class); |
| [451](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L451) | resolveOrganizationRoleScopes 권한이 없으면 빈 리스트 | 호출 resolveOrganizationRoleScopes(memberId) | 성공: 검증 assertThat(scopes).isEmpty(); |
| [471](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L471) | findStudyGroupIds scope 비어있으면 port 호출없이 빈 Set | 호출 findStudyGroupIds(List.of(), 10L) | 성공: 검증 assertThat(result).isEmpty(); |
| [481](../../../src/test/java/com/umc/product/organization/application/service/query/StudyGroupQueryServiceTest.java#L481) | findStudyGroupIds scope 가 있으면 port 위임 | 호출 findStudyGroupIds(scopes, gisuId) | 성공: 검증 assertThat(result).containsExactlyInAnyOrder(100L, 200L); |

### StudyGroupScheduleQueryServiceTest
- 위치: `src/test/java/com/umc/product/organization/application/service/StudyGroupScheduleQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [25](../../../src/test/java/com/umc/product/organization/application/service/StudyGroupScheduleQueryServiceTest.java#L25) | 호출자 시야의 스터디 그룹IDs를 받아 매핑된 일정IDs를 반환 | 호출 findScheduleIdsByStudyGroupIds(visibleGroupIds) | 성공: 검증 assertThat(result).containsExactlyInAnyOrder(100L, 200L, 300L); |
| [44](../../../src/test/java/com/umc/product/organization/application/service/StudyGroupScheduleQueryServiceTest.java#L44) | 권한 없는 사용자는 visible 그룹이 없어 DB 호출 없이 빈 Set | 호출 findScheduleIdsByStudyGroupIds(List.of()) | 성공: 검증 assertThat(result).isEmpty(); |
| [59](../../../src/test/java/com/umc/product/organization/application/service/StudyGroupScheduleQueryServiceTest.java#L59) | 시야의 그룹에 등록된 일정이 없어도 정상 빈 Set 반환 | 호출 findScheduleIdsByStudyGroupIds(visibleGroupIds) | 성공: 검증 assertThat(result).isEmpty(); |

### UmcProductAccessPolicyTest
- 위치: `src/test/java/com/umc/product/organization/application/service/UmcProductAccessPolicyTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [36](../../../src/test/java/com/umc/product/organization/application/service/UmcProductAccessPolicyTest.java#L36) | 중앙 총괄단은 Leadership 조회 없이 조직을 관리할 수 있다 | 중앙 총괄단 역할 보유 | 성공: Leadership과 날짜 조회 없이 허용한다 |
| [46](../../../src/test/java/com/umc/product/organization/application/service/UmcProductAccessPolicyTest.java#L46) | 오늘 유효한 Product Leadership이 있으면 조직을 관리할 수 있다 | KST 오늘에 유효한 Lead/Vice Lead | 성공: 관리를 허용한다 |
| [60](../../../src/test/java/com/umc/product/organization/application/service/UmcProductAccessPolicyTest.java#L60) | 오늘 유효한 Product Leadership이 없으면 조직을 관리할 수 없다 | 기준일에 유효한 Leadership 없음 | 성공: 관리를 거부한다 |
| [74](../../../src/test/java/com/umc/product/organization/application/service/UmcProductAccessPolicyTest.java#L74) | 본인은 Leadership이 없어도 자신의 프로필을 관리할 수 있다 | 요청자와 프로필 소유자가 같음 | 성공: 별도 권한 조회 없이 허용한다 |

### UmcProductMemberCommandServiceTest
- 위치: `src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [78](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceTest.java#L78) | 본인은 Leadership이 없어도 프로필을 수정할 수 있다 | 본인 프로필 수정 | 성공: 멤버 프로필을 저장한다 |
| [95](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceTest.java#L95) | 관리 권한이 없으면 본인의 활동 기간도 추가할 수 없다 | 본인이나 Product 관리 권한 없음 | 실패: `UMC_PRODUCT_ACCESS_DENIED` |
| [115](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceTest.java#L115) | 기존 활동 기간과 겹치거나 인접한 기간은 추가할 수 없다 | 저장된 기간과 overlap 또는 adjacency 발생 | 실패: `UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED` |
| [139](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceTest.java#L139) | 멤버 생성 시 서로 겹치는 활동 기간을 등록할 수 없다 | 생성 요청 안의 두 기간이 중첩 | 실패: `UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED` |
| [150](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceTest.java#L150) | 멤버 생성 시 빈 날짜 없이 인접한 활동 기간을 등록할 수 없다 | 이전 종료일 다음 날에 새 기간 시작 | 실패: `UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED` |

### UmcProductDateProviderTest
- 위치: `src/test/java/com/umc/product/organization/application/service/UmcProductDateProviderTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [17](../../../src/test/java/com/umc/product/organization/application/service/UmcProductDateProviderTest.java#L17) | KST 자정 직전에는 이전 날짜를 반환한다 | UTC `14:59:59` 고정 Clock | 성공: KST 이전 날짜를 반환한다 |
| [26](../../../src/test/java/com/umc/product/organization/application/service/UmcProductDateProviderTest.java#L26) | KST 자정부터는 다음 날짜를 반환한다 | UTC `15:00:00` 고정 Clock | 성공: KST 다음 날짜를 반환한다 |

## Repository / Outbound Persistence

### StudyGroupPersistenceAdapterTest
- 위치: `src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupPersistenceAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [32](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupPersistenceAdapterTest.java#L32) | save 도메인 정상경로로 생성한 그룹은 자식까지 cascade로 보존된다 | 호출 save(group) | 성공: 검증 assertThat(reloaded.getMembers()); .containsExactlyInAnyOrder(100L, 101L); assertThat(reloaded.getMentors()); .containsExactly(200L); |
| [54](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupPersistenceAdapterTest.java#L54) | getEntityById 존재하지 않으면 OrganizationDomainException STUDY GROUP NOT FOUND | 호출 getEntityById(99999L)) | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_NOT_FOUND; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_NOT_FOUND); |
| [63](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupPersistenceAdapterTest.java#L63) | getByName 존재하지 않으면 OrganizationDomainException STUDY GROUP NOT FOUND | 호출 getByName("존재하지_않는_그룹")) | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_NOT_FOUND; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_NOT_FOUND); |

### StudyGroupQueryRepositoryTest
- 위치: `src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [36](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L36) | findStudyGroupHeaders AsSchoolCore scope 학교 멤버가 멤버로 등록된 그룹만 반환 | 호출 findStudyGroupHeaders(scopes, gisuId, null, 20) | 성공: 검증 assertThat(result).extracting(StudyGroupHeaderInfo::groupId); .containsExactlyInAnyOrder(hit1.getId(), hit2.getId()) |
| [64](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L64) | findStudyGroupHeaders AsPartLeader scope memberId가 멘토로 등록된 그룹만 반환 | 호출 findStudyGroupHeaders(scopes, gisuId, null, 20) | 성공: 검증 assertThat(result).extracting(StudyGroupHeaderInfo::groupId); .containsExactly(hit.getId()) |
| [90](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L90) | findStudyGroupHeaders 두 scope OR 결합 중복은 한 번만 반환 | 호출 findStudyGroupHeaders(scopes, gisuId, null, 20) | 실패: 검증 assertThat(result).extracting(StudyGroupHeaderInfo::groupId); .containsExactlyInAnyOrder(schoolOnly.getId(), mentorOnly.getId(), both.getId()); |
| [121](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L121) | findStudyGroupHeaders 다른 기수 그룹은 제외 | 호출 findStudyGroupHeaders(scopes, activeGisu, null, 20) | 성공: 검증 assertThat(result).extracting(StudyGroupHeaderInfo::groupId); .containsExactly(activeGroup.getId()) |
| [147](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L147) | findStudyGroupHeaders 커서 페이지네이션 id DESC로 lt cursor만 반환 | 호출 findStudyGroupHeaders(scopes, gisuId, newest.getId(), 20) | 성공: 검증 assertThat(result).extracting(StudyGroupHeaderInfo::groupId); .containsExactly(middle.getId(), oldest.getId()) |
| [174](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L174) | findStudyGroupHeaders 모든 scope의 predicate가 null이면 빈 결과 | 호출 findStudyGroupHeaders(scopes, gisuId, null, 20) | 성공: 검증 assertThat(result).isEmpty(); |
| [192](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L192) | findMemberIdsByStudyGroupIds 그룹별 memberIds 매핑 | 호출 findMemberIdsByStudyGroupIds(List.of(g1.getId(), g2.getId())) | 성공: 검증 assertThat(result).containsOnlyKeys(g1.getId(), g2.getId()); assertThat(result.get(g1.getId())).containsExactlyInAnyOrder(100L, 101L); assertThat(result.get(g2.getId())).containsExactly(200L); assertThat(result).doesN... |
| [215](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L215) | findMentorIdsByStudyGroupIds 그룹별 mentorIds 매핑 | 호출 findMentorIdsByStudyGroupIds(List.of(g1.getId(), g2.getId())) | 성공: 검증 assertThat(result.get(g1.getId())).containsExactlyInAnyOrder(10L, 11L); assertThat(result.get(g2.getId())).containsExactly(20L); |
| [234](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L234) | findMemberIdsByStudyGroupIds 빈 입력은 빈 맵 | 호출 findMemberIdsByStudyGroupIds(List.of())).isEmpty(); 호출 findMentorIdsByStudyGroupIds(List.of())).isEmpty() | 성공: 검증 assertThat(sut.findMemberIdsByStudyGroupIds(List.of())).isEmpty(); assertThat(sut.findMentorIdsByStudyGroupIds(List.of())).isEmpty(); |
| [240](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L240) | findEntityById mentors와 members 컬렉션을 초기화한 상태로 반환 | 호출 findEntityById(target.getId()) | 성공: 검증 assertThat(result).isPresent(); assertThat(loaded.getMembers()); .containsExactlyInAnyOrder(100L, 101L); assertThat(loaded.getMentors()) |
| [266](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L266) | findEntityById 존재하지 않으면 Optional empty | 호출 findEntityById(99999L) | 성공: 검증 assertThat(result).isEmpty(); |
| [275](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L275) | findStudyGroupHeaders AsSchoolCore 학교 멤버가 멘토로 등록된 그룹도 포함 | 호출 findStudyGroupHeaders(scopes, gisuId, null, 20) | 성공: 검증 assertThat(result).extracting(StudyGroupHeaderInfo::groupId); .containsExactlyInAnyOrder(groupByMember.getId(), groupByMentor.getId()) |
| [302](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L302) | findStudyGroupIds scope 적용된 studyGroupIds Set 반환 | 호출 findStudyGroupIds(scopes, gisuId) | 성공: 검증 assertThat(result); .containsExactlyInAnyOrder(hit1.getId(), hit2.getId()) |
| [329](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L329) | findStudyGroupIds scope predicate null이면 빈 Set | 호출 findStudyGroupIds(List.of(new AsSchoolCore(Set.of())), gisuId) | 성공: 검증 assertThat(result).isEmpty(); |
| [344](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L344) | findStudyGroupIds 다른 기수 그룹은 제외 | 호출 findStudyGroupIds(List.of(new AsPartLeader(me)), activeGisu) | 성공: 검증 assertThat(result).containsExactly(activeGroup.getId()).doesNotContain(otherGroup.getId()); |

### StudyGroupSchedulePersistenceAdapterTest
- 위치: `src/test/java/com/umc/product/organization/adapter/out/persistence/StudyGroupSchedulePersistenceAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [26](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/StudyGroupSchedulePersistenceAdapterTest.java#L26) | 회장단 시야의 스터디 그룹들에 등록된 일정ID들을 batch로 가져온다 | 조건 회장단 시야의 스터디 그룹들에 등록된 일정ID들을 batch로 가져온다 | 성공: 검증 assertThat(visibleScheduleIds); .containsExactlyInAnyOrder(100L, 101L, 200L) |
| [56](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/StudyGroupSchedulePersistenceAdapterTest.java#L56) | 파트장 시야 그룹의 일정ID들을 가져온다 | 호출 findScheduleIdsByStudyGroupIds(List.of(myMentorGroup)) | 성공: 검증 assertThat(visibleScheduleIds).containsExactlyInAnyOrder(100L, 101L, 102L); |
| [75](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/StudyGroupSchedulePersistenceAdapterTest.java#L75) | 스터디 그룹은 있지만 등록된 일정이 없으면 빈 결과 | 호출 findScheduleIdsByStudyGroupIds(List.of(groupWithNoSchedule)) | 성공: 검증 assertThat(result).isEmpty(); |
| [88](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/StudyGroupSchedulePersistenceAdapterTest.java#L88) | 권한 없는 사용자는 visible 그룹이 없어 빈 입력 DB 호출 없이 즉시 종료 | 호출 findScheduleIdsByStudyGroupIds(List.of()) | 성공: 검증 assertThat(result).isEmpty(); |

### UmcProductPersistenceAdapterTest
- 위치: `src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [78](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L78) | 활동 기간의 LocalDate는 DATE로 그대로 왕복하고 종료일을 포함한다 | 저장 후 영속성 context 초기화 및 재조회 | 성공: 시작일·종료일과 포함 경계를 유지한다 |
| [126](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L126) | 멤버 활동 기간은 겹치거나 빈틈없이 인접할 수 없다 | 기존 종료일 다음 날 새 기간 시작 | 실패: `OrganizationDomainException`, `UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED` |
| [153](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L153) | Chapter는 활성 필터와 정렬 순서를 유지한다 | 활성·비활성 Chapter 저장 | 성공: 활성 Chapter만 정렬 조회한다 |
| [166](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L166) | 멤버는 같은 기간에 여러 Chapter에 소속될 수 있다 | 동일 멤버 기간에 Develop과 Client Chapter 소속 생성 | 성공: 두 Chapter 소속을 모두 조회한다 |
| [199](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L199) | 멤버 검색의 activeOn은 멤버와 Chapter 소속 기간을 모두 검사한다 | 현재 활동 멤버와 종료된 멤버 검색 | 성공: 기준일 유효 멤버만 반환한다 |
| [242](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L242) | 같은 멤버의 같은 Chapter 소속 기간은 겹칠 수 없다 | 종료일을 공유하는 두 Chapter 소속 | 실패: `OrganizationDomainException`, `UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED` |
| [282](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L282) | Leadership은 멤버와 자체 기간이 모두 유효한 날에만 조회된다 | Leadership 종료일과 다음 날 조회 | 성공: 종료일만 유효하게 반환한다 |
| [361](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L361) | 동일한 Leadership 역할은 같은 날 한 명만 가질 수 있다 | 같은 날짜의 두 Vice Lead | 실패: `OrganizationDomainException`, `UMC_PRODUCT_LEADERSHIP_OVERLAPPED` |
| [337](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L337) | Squad activeOn은 종료일을 포함하고 기간 밖 Squad를 제외한다 | 현재·미래·비활성 Squad 조회 | 성공: 기준일에 유효한 활성 Squad만 반환한다 |
| [355](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L355) | 같은 Squad의 멤버 참여와 SquadLead 기간 중복을 조회한다 | 기존 SquadLead 종료일에 새 구간 검사 | 성공: 멤버·리더 중복을 모두 감지한다 |
| [393](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterTest.java#L393) | 멤버 하위 활동을 FK 안전 순서로 일괄 삭제할 수 있다 | 참여·소속·Leadership·활동 기간 순서로 삭제 | 성공: 멤버까지 삭제된다 |

## Domain

### GisuActivityDaysTest
- 테스트 설명: Gisu.activityDays 도메인 메서드
- 위치: `src/test/java/com/umc/product/organization/domain/GisuActivityDaysTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [18](../../../src/test/java/com/umc/product/organization/domain/GisuActivityDaysTest.java#L18) | Gisu.activityDays 도메인 메서드 / now가 시작일 이전이면 0일을 반환한다 | 조건 Gisu.activityDays 도메인 메서드 / now가 시작일 이전이면 0일을 반환한다 | 성공: 검증 assertThat(days).isZero(); |
| [28](../../../src/test/java/com/umc/product/organization/domain/GisuActivityDaysTest.java#L28) | Gisu.activityDays 도메인 메서드 / now가 시작일 직전 경계이면 0일을 반환한다 | 조건 Gisu.activityDays 도메인 메서드 / now가 시작일 직전 경계이면 0일을 반환한다 | 성공: 검증 assertThat(days).isZero(); |
| [37](../../../src/test/java/com/umc/product/organization/domain/GisuActivityDaysTest.java#L37) | Gisu.activityDays 도메인 메서드 / now가 시작일 정각이면 0일을 반환한다 | 조건 Gisu.activityDays 도메인 메서드 / now가 시작일 정각이면 0일을 반환한다 | 성공: 검증 assertThat(days).isZero(); |
| [46](../../../src/test/java/com/umc/product/organization/domain/GisuActivityDaysTest.java#L46) | Gisu.activityDays 도메인 메서드 / 진행중인 기수는 now까지의 일수를 반환한다 | 조건 Gisu.activityDays 도메인 메서드 / 진행중인 기수는 now까지의 일수를 반환한다 | 성공: 검증 assertThat(days).isEqualTo(45L); |
| [56](../../../src/test/java/com/umc/product/organization/domain/GisuActivityDaysTest.java#L56) | Gisu.activityDays 도메인 메서드 / 종료된 기수는 전체 기간을 반환한다 | 조건 Gisu.activityDays 도메인 메서드 / 종료된 기수는 전체 기간을 반환한다 | 성공: 검증 assertThat(days).isEqualTo(expected); |
| [67](../../../src/test/java/com/umc/product/organization/domain/GisuActivityDaysTest.java#L67) | Gisu.activityDays 도메인 메서드 / now가 정확히 종료일이면 전체 기간을 반환한다 | 조건 Gisu.activityDays 도메인 메서드 / now가 정확히 종료일이면 전체 기간을 반환한다 | 성공: 검증 assertThat(days).isEqualTo(expected); |

### StudyGroupTest
- 위치: `src/test/java/com/umc/product/organization/domain/StudyGroupTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [17](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L17) | create 정상 입력이면 StudyGroup 생성 | 조건 create 정상 입력이면 StudyGroup 생성 | 성공: 검증 assertThat(group.getName()).isEqualTo("스프링 스터디"); assertThat(group.getGisuId()).isEqualTo(1L); assertThat(group.getPart()).isEqualTo(ChallengerPart.SPRINGBOOT); assertThat(group.getMembers()).isEmpty(); |
| [30](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L30) | create name 이 blank면 STUDY GROUP NAME REQUIRED | 조건 create name 이 blank면 STUDY GROUP NAME REQUIRED | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_NAME_REQUIRED; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_NAME_REQUIRED); |
| [38](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L38) | create gisuId 가 null이면 GISU REQUIRED | 조건 create gisuId 가 null이면 GISU REQUIRED | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.GISU_REQUIRED; 검증 .isEqualTo(OrganizationErrorCode.GISU_REQUIRED); |
| [46](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L46) | create part 가 null이면 PART REQUIRED | 조건 create part 가 null이면 PART REQUIRED | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.PART_REQUIRED; 검증 .isEqualTo(OrganizationErrorCode.PART_REQUIRED); |
| [54](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L54) | addMember 정상이면 members에 추가된다 | 조건 addMember 정상이면 members에 추가된다 | 성공: 검증 assertThat(group.getMembers()); .containsExactly(100L, 101L); |
| [69](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L69) | addMember 이미 존재하는 memberId면 STUDY GROUP MEMBER DUPLICATED | 조건 addMember 이미 존재하는 memberId면 STUDY GROUP MEMBER DUPLICATED | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_MEMBER_DUPLICATED; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_DUPLICATED); |
| [82](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L82) | assignMentor 이미 존재하는 mentorId면 STUDY GROUP MENTOR DUPLICATED | 조건 assignMentor 이미 존재하는 mentorId면 STUDY GROUP MENTOR DUPLICATED | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_MENTOR_DUPLICATED; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MENTOR_DUPLICATED); |
| [95](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L95) | removeMember 존재하지 않는 memberId면 STUDY GROUP MEMBER NOT FOUND | 조건 removeMember 존재하지 않는 memberId면 STUDY GROUP MEMBER NOT FOUND | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_MEMBER_NOT_FOUND; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_NOT_FOUND); |
| [108](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L108) | removeMember 마지막 멤버를 제거하면 STUDY GROUP MEMBER REQUIRED | 조건 removeMember 마지막 멤버를 제거하면 STUDY GROUP MEMBER REQUIRED | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_MEMBER_REQUIRED; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_REQUIRED); |
| [121](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L121) | removeMember 2명 이상일 때는 정상 제거 | 조건 removeMember 2명 이상일 때는 정상 제거 | 성공: 검증 assertThat(group.getMembers()); .containsExactly(101L); |
| [136](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L136) | removeMentor 마지막 멘토를 제거하면 STUDY GROUP MENTOR REQUIRED | 조건 removeMentor 마지막 멘토를 제거하면 STUDY GROUP MENTOR REQUIRED | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_MENTOR_REQUIRED; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MENTOR_REQUIRED); |
| [149](../../../src/test/java/com/umc/product/organization/domain/StudyGroupTest.java#L149) | addMembers 빈 Set이면 STUDY GROUP MEMBER REQUIRED | 조건 addMembers 빈 Set이면 STUDY GROUP MEMBER REQUIRED | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.STUDY_GROUP_MEMBER_REQUIRED; 검증 .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_REQUIRED); |

### UmcProductDatePeriodTest
- 위치: `src/test/java/com/umc/product/organization/domain/UmcProductDatePeriodTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [20](../../../src/test/java/com/umc/product/organization/domain/UmcProductDatePeriodTest.java#L20) | 같은 날 시작하고 종료하는 기간을 생성할 수 있다 | 동일한 `LocalDate` 시작일과 종료일 | 성공: 종료일을 포함해 활성 상태로 판단한다 |
| [29](../../../src/test/java/com/umc/product/organization/domain/UmcProductDatePeriodTest.java#L29) | 종료일이 없는 진행 중 기간을 생성할 수 있다 | `endDate=null` | 성공: 미래 날짜에도 활성 상태로 판단한다 |
| [37](../../../src/test/java/com/umc/product/organization/domain/UmcProductDatePeriodTest.java#L37) | 시작일이 없으면 기간을 생성할 수 없다 | `startDate=null` | 실패: `UMC_PRODUCT_START_DATE_REQUIRED` |
| [45](../../../src/test/java/com/umc/product/organization/domain/UmcProductDatePeriodTest.java#L45) | 종료일이 시작일보다 빠르면 기간을 생성할 수 없다 | `endDate < startDate` | 실패: `UMC_PRODUCT_PERIOD_INVALID` |
| [53](../../../src/test/java/com/umc/product/organization/domain/UmcProductDatePeriodTest.java#L53) | 기간 포함과 겹침은 종료일을 포함해서 판단한다 | 종료일이 같은 두 기간 | 성공: 포함 및 겹침으로 판단한다 |
| [62](../../../src/test/java/com/umc/product/organization/domain/UmcProductDatePeriodTest.java#L62) | 빈 날짜 없이 이어진 기간은 인접한 기간이다 | 이전 종료일 다음 날에 시작 | 성공: 중첩 없이 인접으로 판단한다 |

### UmcProductMemberActivityTest
- 위치: `src/test/java/com/umc/product/organization/domain/UmcProductMemberActivityTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [21](../../../src/test/java/com/umc/product/organization/domain/UmcProductMemberActivityTest.java#L21) | 멤버 활동 기간과 그 안의 Chapter 소속을 생성한다 | Chapter 소속 기간이 멤버 기간 안에 있음 | 성공: Chapter·책임·기간을 보존한다 |
| [45](../../../src/test/java/com/umc/product/organization/domain/UmcProductMemberActivityTest.java#L45) | Chapter 소속 기간은 멤버 활동 기간 안에 있어야 한다 | 소속 시작일이 멤버 시작일보다 빠름 | 실패: `UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE` |
| [63](../../../src/test/java/com/umc/product/organization/domain/UmcProductMemberActivityTest.java#L63) | Product Leadership은 Chapter 소속과 독립적으로 생성한다 | 별도 Chapter membership 없이 Leadership 생성 | 성공: Product Lead 이력을 독립적으로 보존한다 |
| [83](../../../src/test/java/com/umc/product/organization/domain/UmcProductMemberActivityTest.java#L83) | Product Leadership 기간은 멤버 활동 기간 안에 있어야 한다 | 무기한 Leadership이 유한 멤버 기간을 벗어남 | 실패: `UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE` |

### UmcProductOrganizationStructureTest
- 위치: `src/test/java/com/umc/product/organization/domain/UmcProductOrganizationStructureTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [15](../../../src/test/java/com/umc/product/organization/domain/UmcProductOrganizationStructureTest.java#L15) | 조직 역할은 Product Leadership만 제공한다 | Leadership role enum 조회 | 성공: Lead와 Vice Lead만 존재한다 |
| [24](../../../src/test/java/com/umc/product/organization/domain/UmcProductOrganizationStructureTest.java#L24) | Chapter를 생성한다 | Chapter 표시 정보 입력 | 성공: 공백을 정리해 저장한다 |
| [33](../../../src/test/java/com/umc/product/organization/domain/UmcProductOrganizationStructureTest.java#L33) | Chapter의 코드와 이름은 필수다 | 공백 코드 또는 이름 | 실패: 필수값 오류 코드를 반환한다 |
| [45](../../../src/test/java/com/umc/product/organization/domain/UmcProductOrganizationStructureTest.java#L45) | Chapter를 수정한다 | 표시·활성 정보 수정 | 성공: 변경 값을 반영한다 |

### UmcProductSquadTest
- 위치: `src/test/java/com/umc/product/organization/domain/UmcProductSquadTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [21](../../../src/test/java/com/umc/product/organization/domain/UmcProductSquadTest.java#L21) | Squad는 필수 시작일과 nullable 종료일을 가진다 | `LocalDate startDate`, `endDate=null` | 성공: 미래 날짜에도 활성 상태로 판단한다 |
| [38](../../../src/test/java/com/umc/product/organization/domain/UmcProductSquadTest.java#L38) | Squad 종료일이 시작일보다 빠르면 생성할 수 없다 | `endDate < startDate` | 실패: `UMC_PRODUCT_PERIOD_INVALID` |
| [54](../../../src/test/java/com/umc/product/organization/domain/UmcProductSquadTest.java#L54) | Squad 참여 기간은 멤버와 Squad 기간 모두에 포함되어야 한다 | 참여 기간이 두 상위 기간을 벗어남 | 실패: `UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE` |
| [78](../../../src/test/java/com/umc/product/organization/domain/UmcProductSquadTest.java#L78) | Squad 참여자는 역할과 책임, 자체 기간을 가진다 | SquadLead 참여 생성 | 성공: 역할·책임·`LocalDate` 기간을 보존한다 |
