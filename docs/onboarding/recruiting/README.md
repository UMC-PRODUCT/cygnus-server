# Recruiting 온보딩

Recruiting 도메인의 설계와 검증 근거를 찾기 위한 진입점이다.

- [도메인 구조와 흐름](../domain/recruiting.md)
- [엔티티 관계 및 사용자 흐름 다이어그램](../domain/recruiting-diagrams.md)
- [테스트 전략](../test/recruiting.md)
- [전체 테스트 케이스 카탈로그](test-cases.md)

지부 조건이 필요한 시즌·차수 조회는 시즌에 지부 ID를 저장하지 않는다. `RecruitingSeason.schoolId`와 해당 기수의 현재 학교-지부 관계를 조회 시점에 조합하므로 학교의 지부가 바뀌어도 별도 데이터 보정 없이 최신 소속을 반환한다.
