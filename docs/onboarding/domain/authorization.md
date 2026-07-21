# Authorization Domain

## 역할

`authorization` 도메인은 리소스별 접근 권한을 평가한다. `@CheckAccess`, `ResourceType`, `PermissionType`, 역할 정보를 통해 사용자가 어떤 작업을 할 수 있는지 판단한다.

## 책임

- 리소스 타입과 권한 타입의 조합을 검증한다.
- 권한 평가기를 찾아 권한을 확인한다.
- 챌린저 역할과 조직 범위를 기반으로 접근 가능 여부를 판단한다.
- 권한 실패 시 일관된 메시지를 제공한다.

## 경계

권한 도메인은 접근 가능 여부를 판단할 뿐, 대상 리소스의 상태 변경은 각 도메인의 Command UseCase가 책임진다. `adapter/in`은 `@CheckAccess`로 권한을 선언하고, 세부 규칙은 evaluator에 둔다.

`SUPER_ADMIN` 같은 회원 전역 system role의 저장 책임은 `member` 도메인에 있다. `authorization` 도메인은 공개 Query UseCase로 system role을 읽고, 기수별 ChallengerRole과 함께 `AuthoritySnapshot`으로 구성하여 권한만 평가한다.

권한 저장소 migration, cache schema 변경, 서버 다중화 또는 rolling deployment를 검토할 때는 [Authorization 운영 안전 가이드](authorization-operational-safety.md)를 따른다.

## UX Writing Notes

권한 오류는 사용자가 조심해야 하는 상황이다. `권한이 없어요`에서 끝내지 말고 `필요한 권한이 있다면 운영진에게 문의해주세요`를 함께 제공한다.
