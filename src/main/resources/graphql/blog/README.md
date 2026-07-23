# Blog GraphQL IDL

`BlogContent`, `BlogSeries`, `BlogHashtag`, `BlogComment`는 Blog가 소유하는 canonical resource다.
작성자는 별도 Blog author 타입을 만들지 않고 `MemberPublic`으로 연결한다.

cursor connection은 공개 정렬 순서를 보존한다. `viewer`는 resource 자체가 아니라 현재 요청자의
좋아요·편집 권한을 계산한 projection이다.
