# Curriculum GraphQL IDL

`Curriculum`과 `WeeklyCurriculum`은 기수·파트 교육 계획을 나타낸다. 공개 개요와 현재 Member의
진행 상황은 같은 resource를 반환하며, 개인 진행 조회에서만 `workbooks`와 `submission`이 채워진다.

`OriginalWorkbook`은 원본 과제 정의, `ChallengerWorkbook`은 Challenger에게 배포된 실행 instance다.
피드백 작성자는 `MemberPublic`, workbook 소유자는 `Challenger`로 연결한다.
