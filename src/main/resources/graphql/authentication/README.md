# Authentication GraphQL IDL

Authentication GraphQL은 인증 프로토콜 자체가 아니라 현재 Member와 연결된 OAuth resource와
credential 사전 검사만 제공한다. OAuth authorize redirect, authorization code 교환, token 발급과
logout은 HTTP status, cookie, redirect 의미가 필요하므로 REST/SSO transport가 계속 소유한다.

`MemberOAuth.memberId`는 Member 식별자이며 `member`는 `MemberPublic` provider resource다.
