# Term GraphQL IDL

`Term`은 약관 종류별 version resource다. `terms`는 현재 활성 version을, `myAgreedTerms`는
Member가 동의한 version snapshot을 반환한다.

`RequiredTermConsentStatus`는 현재 필수 version과 Member 동의 이력의 차이를 계산한 projection이다.
