# Audit GraphQL IDL

`AuditLog`는 감사 이벤트의 불변 기록을 나타낸다. `actorMemberId`는 Member 식별자이며
`actor`는 같은 의미의 `MemberPublic` provider resource를 selection에 따라 조회한다.

감사 로그는 `ResourceType.AUDIT`의 `READ` 권한이 있는 요청자만 조회할 수 있다.
