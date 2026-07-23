# Certificate GraphQL IDL

`Certificate`는 Member에게 발급된 인증서 resource다. `gisuId`는 Organization `Gisu`를
참조하고 `download`는 현재 요청자 권한을 검사한 뒤 짧게 유효한 접근 URL을 계산한다.

PDF binary preview와 download stream은 GraphQL payload에 포함하지 않는다. GraphQL은
metadata와 signed download URL만 제공한다.
