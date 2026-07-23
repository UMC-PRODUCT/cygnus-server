# Notice GraphQL IDL

`Notice`는 공지 본문과 target, vote, attachment link를 소유한다. target의 Gisu·Chapter·School과
작성자는 각각 Organization과 Member provider resource로 연결한다.

목록 filter는 요청자의 기수·조직·운영 역할 context와 함께 평가되며, 클라이언트가 높은 role
tab을 지정해도 서버가 실제 보유 권한을 기준으로 제한한다.
