# Community GraphQL IDL

`CommunityPost`와 `CommunityComment`는 Community가 소유한다. 작성자 자체는 Member와
Challenger resource를 직접 참조하며, 작성 시점 이름·프로필 snapshot은 canonical contract에
중복 노출하지 않는다.

`lightning`은 `LIGHTNING` category에서만 존재하는 domain field이고 `viewer`는 현재 요청자의
좋아요·스크랩 상태 projection이다.
