# Blog 테스트 케이스

- 테스트 파일: 3개
- 테스트 케이스: 33개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| UseCase / Application Service | 1 |
| E2E / Integration | 15 |
| Domain | 17 |

## UseCase / Application Service

### CreateBlogCommentCommandTest
- 테스트 설명: Blog 댓글 생성 Command 테스트
- 위치: `src/test/java/com/umc/product/blog/application/port/in/command/dto/CreateBlogCommentCommandTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [9](../../../src/test/java/com/umc/product/blog/application/port/in/command/dto/CreateBlogCommentCommandTest.java#L9) | 닉네임은 trim 후 20자 이하만 허용한다 | 조건 닉네임은 trim 후 20자 이하만 허용한다 | 실패: 예외 BlogDomainException; 검증 assertThat(command.nickname()).isEqualTo("익명작성자"); |

## E2E / Integration

### BlogInteractionControllerIntegrationTest
- 테스트 설명: BlogInteractionController 통합 테스트
- 위치: `src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [103](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L103) | BlogInteractionController 통합 테스트 / 슈퍼 관리자는 콘텐츠를 생성하고 public 상세와 해시태그로 조회할 수 있다 | HTTP POST /api/v1/blog/contents; HTTP GET /api/v1/blog/contents/engineering/cms-post; HTTP GET /api/v1/blog/hashtags/springboot/contents | 실패: HTTP 403 Forbidden; HTTP 200 OK |
| [133](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L133) | SEO 경로 목록은 공개 콘텐츠 경로를 반환한다 | HTTP GET /api/v1/blog/seo/paths | 성공: HTTP 200 OK |
| [142](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L142) | 공개 콘텐츠 목록은 공개되지 않은 콘텐츠 ID 커서를 거부한다 | HTTP GET /api/v1/blog/contents; query cursor=draft.getId( | 실패: HTTP 400 Bad Request |
| [164](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L164) | 해시태그 목록 커서는 type 필터 기준의 콘텐츠 수로 계산된다 | HTTP GET /api/v1/blog/hashtags; query type="engineering"; query size="1"; query sort="contentCount,desc"; query cursor=Long.toString(nextCursor | 성공: HTTP 200 OK; 검증 assertThat(secondResult.path("content").get(0).path("id").asLong()).isNotEqualTo(firstHashtagId); |
| [202](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L202) | 시리즈 콘텐츠 목록은 공개되지 않은 콘텐츠 ID 커서를 거부한다 | HTTP PUT /api/v1/blog/series/; HTTP GET /api/v1/blog/series/engineering/cursor-series/contents; query cursor=draft.getId( | 실패: HTTP 200 OK; HTTP 400 Bad Request |
| [243](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L243) | 콘텐츠가 없어도 댓글 목록은 빈 커서 응답을 반환한다 | 조건 콘텐츠가 없어도 댓글 목록은 빈 커서 응답을 반환한다 | 성공: HTTP 200 OK |
| [254](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L254) | 콘텐츠 좋아요는 없는 콘텐츠 조회 시 빈 상태를 반환하고 공개 콘텐츠에서 토글된다 | 조건 콘텐츠 좋아요는 없는 콘텐츠 조회 시 빈 상태를 반환하고 공개 콘텐츠에서 토글된다 | 성공: HTTP 200 OK |
| [283](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L283) | 본인 댓글만 수정할 수 있고 수정된 내용이 조회된다 | 조건 본인 댓글만 수정할 수 있고 수정된 내용이 조회된다 | 실패: HTTP 200 OK; HTTP 403 Forbidden |
| [344](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L344) | 댓글 목록은 size와 createdAt 정렬 기준에 맞춰 커서 페이지네이션된다 | query sort="createdAt,asc"; query size="2"; query cursor=secondId.toString(; query sort="createdAt,desc" | 성공: HTTP 200 OK |
| [378](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L378) | 삭제된 댓글 ID를 커서로 사용하면 400을 반환한다 | query sort="createdAt,asc"; query size="2"; query cursor=secondId.toString( | 실패: HTTP 200 OK; HTTP 400 Bad Request |
| [402](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L402) | 지원하지 않는 댓글 정렬 조건은 400을 반환한다 | query sort="likeCount,desc" | 실패: HTTP 400 Bad Request |
| [412](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L412) | 대댓글이 있는 부모 댓글을 본인이 삭제하면 부모는 삭제 placeholder로 남고 대댓글은 조회된다 | 조건 대댓글이 있는 부모 댓글을 본인이 삭제하면 부모는 삭제 placeholder로 남고 대댓글은 조회된다 | 실패: HTTP 200 OK; HTTP 400 Bad Request |
| [444](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L444) | 삭제된 부모의 모든 대댓글이 사라지면 부모 댓글도 목록에서 제외된다 | 조건 삭제된 부모의 모든 대댓글이 사라지면 부모 댓글도 목록에서 제외된다 | 성공: HTTP 200 OK |
| [464](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L464) | 관리자 삭제는 관리자 placeholder로 조회되고 일반 사용자는 관리자 삭제를 호출할 수 없다 | 조건 관리자 삭제는 관리자 placeholder로 조회되고 일반 사용자는 관리자 삭제를 호출할 수 없다 | 실패: HTTP 403 Forbidden; HTTP 200 OK |
| [487](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogInteractionControllerIntegrationTest.java#L487) | 슈퍼 관리자가 본인 댓글을 삭제하면 본인 삭제 placeholder로 조회된다 | 조건 슈퍼 관리자가 본인 댓글을 삭제하면 본인 삭제 placeholder로 조회된다 | 성공: HTTP 200 OK |

## Domain

### BlogDomainTest
- 테스트 설명: Blog 도메인 테스트
- 위치: `src/test/java/com/umc/product/blog/domain/BlogDomainTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [11](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L11) | 콘텐츠는 CMS 필드와 공개 상태를 검증하고 생성한다 | 조건 콘텐츠는 CMS 필드와 공개 상태를 검증하고 생성한다 | 실패: 예외 BlogDomainException; 검증 assertThat(content.getTitle()).isEqualTo("제목"); assertThat(content.getContent()).isEqualTo("본문"); assertThat(content.isPublished()).isTrue(); assertThat(content.getPublishedAt()).isNotNull(); |
| [51](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L51) | Blog 도메인 테스트 / 콘텐츠와 시리즈 slug는 URL-safe 형식만 허용한다 | 조건 Blog 도메인 테스트 / 콘텐츠와 시리즈 slug는 URL-safe 형식만 허용한다 | 실패: 예외 BlogDomainException |
| [93](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L93) | Blog 도메인 테스트 / 콘텐츠는 DRAFT 전환 시 publishedAt을 제거하고 soft delete된다 | 조건 Blog 도메인 테스트 / 콘텐츠는 DRAFT 전환 시 publishedAt을 제거하고 soft delete된다 | 성공: 검증 assertThat(content.isPublished()).isFalse(); assertThat(content.getPublishedAt()).isNull(); assertThat(content.isDeleted()).isTrue(); assertThat(content.getDeletedAt()).isNotNull(); |
| [121](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L121) | 시리즈는 slug와 작성자를 가진다 | 조건 시리즈는 slug와 작성자를 가진다 | 성공: 검증 assertThat(series.getSlug()).isEqualTo("spring-series"); assertThat(series.isAuthor(1L)).isTrue(); assertThat(series.canonicalPath()).isEqualTo("/series/engineering/spring-series"); |
| [141](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L141) | 시리즈 콘텐츠는 유효한 표시 순서로 생성한다 | 조건 시리즈 콘텐츠는 유효한 표시 순서로 생성한다 | 실패: 예외 BlogDomainException; 검증 assertThat(relation.getSeriesId()).isEqualTo(1L); assertThat(relation.getContentId()).isEqualTo(2L); |
| [153](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L153) | 해시태그는 # 제거, NFKC 정규화, 소문자 변환 후 slug를 만든다 | 조건 해시태그는 # 제거, NFKC 정규화, 소문자 변환 후 slug를 만든다 | 실패: 예외 BlogDomainException; 검증 assertThat(hashtag.getName()).isEqualTo("SpringBoot"); assertThat(hashtag.getNormalizedName()).isEqualTo("springboot"); assertThat(hashtag.getSlug()).isEqualTo("springboot"); |
| [166](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L166) | engineering, design, product, release path 값을 콘텐츠 타입으로 파싱한다 | 조건 engineering, design, product, release path 값을 콘텐츠 타입으로 파싱한다 | 성공: 검증 assertThat(BlogContentType.fromPath("engineering")).isEqualTo(BlogContentType.ENGINEERING); assertThat(BlogContentType.fromPath("design")).isEqualTo(BlogContentType.DESIGN); assertThat(BlogContentType.fromPath("product... |
| [175](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L175) | 영문 path 변환은 시스템 로케일에 영향받지 않는다 | 조건 영문 path 변환은 시스템 로케일에 영향받지 않는다 | 성공: 검증 assertThat(BlogContentType.fromPath("ENGINEERING")).isEqualTo(BlogContentType.ENGINEERING); assertThat(content.canonicalPath()).isEqualTo("/engineering/locale-safe"); assertThat(series.canonicalPath()).isEqualTo("/seri... |
| [215](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L215) | 지원하지 않는 콘텐츠 타입은 예외를 던진다 | 조건 지원하지 않는 콘텐츠 타입은 예외를 던진다 | 실패: 예외 BlogDomainException |
| [222](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L222) | 댓글 내용은 trim 후 1자 이상 1000자 이하만 허용한다 | 조건 댓글 내용은 trim 후 1자 이상 1000자 이하만 허용한다 | 실패: 예외 BlogDomainException; 검증 assertThat(comment.getContent()).isEqualTo("정상 댓글"); |
| [235](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L235) | 비회원 댓글은 trim 후 1자 이상 20자 이하 닉네임이 필요하다 | 조건 비회원 댓글은 trim 후 1자 이상 20자 이하 닉네임이 필요하다 | 실패: 예외 BlogDomainException; 검증 assertThat(comment.isAnonymous()).isTrue(); assertThat(comment.getNickname()).isEqualTo("게스트"); |
| [249](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L249) | 회원 댓글도 닉네임이 전달되면 20자 이하만 허용한다 | 조건 회원 댓글도 닉네임이 전달되면 20자 이하만 허용한다 | 실패: 예외 BlogDomainException; 검증 assertThat(comment.getNickname()).isEqualTo("익명작성자"); |
| [260](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L260) | 삭제된 댓글은 삭제 타입별 placeholder를 표시하고 대댓글을 허용하지 않는다 | 조건 삭제된 댓글은 삭제 타입별 placeholder를 표시하고 대댓글을 허용하지 않는다 | 실패: 예외 BlogDomainException; 검증 assertThat(userDeleted.displayContent()).isEqualTo("삭제된 댓글이에요"); assertThat(userDeleted.canReply()).isFalse(); assertThat(adminDeleted.displayContent()).isEqualTo("관리자가 삭제한 댓글이에요"); assertThat(adminDeleted.canReply())... |
| [278](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L278) | 콘텐츠 좋아요는 유효한 콘텐츠 ID와 회원 ID로 생성한다 | 조건 콘텐츠 좋아요는 유효한 콘텐츠 ID와 회원 ID로 생성한다 | 성공: 검증 assertThat(like.getContentId()).isEqualTo(1L); assertThat(like.getMemberId()).isEqualTo(2L); |
| [287](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L287) | 콘텐츠 좋아요는 유효하지 않은 ID를 거부한다 | 조건 콘텐츠 좋아요는 유효하지 않은 ID를 거부한다 | 실패: 예외 BlogDomainException |
| [296](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L296) | 댓글 좋아요는 유효한 댓글 ID와 회원 ID로 생성한다 | 조건 댓글 좋아요는 유효한 댓글 ID와 회원 ID로 생성한다 | 성공: 검증 assertThat(like.getCommentId()).isEqualTo(1L); assertThat(like.getMemberId()).isEqualTo(2L); |
| [305](../../../src/test/java/com/umc/product/blog/domain/BlogDomainTest.java#L305) | 댓글 좋아요는 유효하지 않은 ID를 거부한다 | 조건 댓글 좋아요는 유효하지 않은 ID를 거부한다 | 실패: 예외 BlogDomainException |
