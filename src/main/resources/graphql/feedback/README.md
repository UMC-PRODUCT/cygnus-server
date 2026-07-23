# Feedback GraphQL IDL

`UserFeedbackTemplate`은 Feedback가 소유하는 context/audience 정책과 Form provider의 `Form`
구조를 결합한다. `targetType`은 실제 대상 resource가 아니라 응답자 audience 분류다.

제출 시 서버는 현재 Member가 template audience에 속하는지 다시 검증하고, 답변 저장은 Form
aggregate에 위임한다.
