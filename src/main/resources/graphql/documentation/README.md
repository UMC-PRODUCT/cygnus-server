# Documentation GraphQL IDL

Documentation은 client code generation과 오류 처리를 위한 error code catalog를 제공한다.

- catalog는 빌드 시 생성된 정적 resource의 snapshot이다.
- `code`, HTTP 상태, client action, deprecation 정보는 REST error envelope와 같은 원천을 사용한다.
- REST Docs HTML, GraphiQL, Apollo Sandbox 같은 UI transport는 GraphQL resource로 모델링하지 않는다.
