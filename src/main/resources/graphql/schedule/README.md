# Schedule GraphQL IDL

`Schedule`은 일정 metadata, 위치, 출석 정책을 소유한다. 작성자와 참여자는 `MemberPublic`을
참조하고 `viewer`는 현재 Member의 참여·출석 상태 projection이다.

출석 요청·승인처럼 위치 검증과 상태 전이가 필요한 command는 기존 application use case를
통해 후속 mutation adapter에서 제공하며, canonical 조회 계약은 REST와 같은 권한 정책을 사용한다.
