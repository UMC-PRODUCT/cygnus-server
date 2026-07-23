# Analytics GraphQL IDL

Analytics는 운영 범위에 따라 계산되는 read model을 소유한다.

- `adminAnalytics`는 요청자의 운영 권한을 기준으로 분석 범위를 결정한다.
- 하위 필드는 GraphQL selection에 포함될 때만 각 Query UseCase를 호출한다.
- `gisuId`, `chapterId`, `schoolId`는 Organization 식별자를 참조한다.
- 위험군과 학교 목록은 공통 `PageInput`/`PageInfo`를 사용한다.
- 원천 Challenger·Member·School resource가 필요하면 각 도메인의 표준 field를 별도로 조회한다.
