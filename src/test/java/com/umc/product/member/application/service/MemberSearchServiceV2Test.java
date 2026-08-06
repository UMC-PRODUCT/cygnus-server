package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.CheckChallengerHistoryUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberV2Result;
import com.umc.product.member.application.port.out.SearchMemberPort;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSearchService.searchByV2 — 회원 단위 v2 검색")
class MemberSearchServiceV2Test {

    private static final Long REQUESTER_MEMBER_ID = 1L;

    @Mock SearchMemberPort searchMemberPort;
    @Mock GetMemberUseCase getMemberUseCase;
    @Mock CheckChallengerHistoryUseCase checkChallengerHistoryUseCase;
    @Mock GetChallengerUseCase getChallengerUseCase;
    @Mock GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock GetGisuUseCase getGisuUseCase;

    @InjectMocks MemberSearchService memberSearchService;

    @BeforeEach
    void setUp() {
        given(checkChallengerHistoryUseCase.hasChallengerHistory(REQUESTER_MEMBER_ID)).willReturn(true);
    }

    private MemberInfo profile(Long id, String name) {
        return MemberInfo.builder()
            .id(id)
            .name(name)
            .nickname(name)
            .email("test@test.com")
            .schoolId(1L)
            .schoolName("학교")
            .status(MemberStatus.ACTIVE)
            .build();
    }

    private ChallengerBasicInfo challenger(
        Long id,
        Long memberId,
        Long gisuId,
        ChallengerPart part,
        ChallengerStatus status
    ) {
        return new ChallengerBasicInfo(
            id,
            memberId,
            gisuId,
            part,
            List.of(ChallengerTrack.from(part)),
            status
        );
    }

    private GisuInfo gisu(Long gisuId, Long generation, boolean active) {
        return new GisuInfo(gisuId, generation, Instant.now(), Instant.now().plusSeconds(100), active);
    }

    @Test
    void 같은_회원의_여러_기수_챌린저는_하나의_row로_묶여_반환된다() {
        // given: memberId=10 한 명이 3개 기수 챌린저 보유, 검색 결과 = 회원 1명
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> memberIdPage = new PageImpl<>(List.of(10L), pageable, 1);

        given(searchMemberPort.searchMemberIds(any(), any())).willReturn(memberIdPage);
        given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of(10L, profile(10L, "홍길동")));
        given(getChallengerUseCase.getAllBasicByMemberIds(anySet())).willReturn(Map.of(
            10L, List.of(
                challenger(101L, 10L, 6L, ChallengerPart.WEB, ChallengerStatus.GRADUATED),
                challenger(102L, 10L, 7L, ChallengerPart.SPRINGBOOT, ChallengerStatus.GRADUATED),
                challenger(103L, 10L, 8L, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE)
            )
        ));
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisu(8L, 9L, true)));
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(
            gisu(6L, 7L, false),
            gisu(7L, 8L, false),
            gisu(8L, 9L, true)
        ));
        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());

        // when
        SearchMemberV2Result result = memberSearchService.searchByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        // then: 회원 1명 = row 1개, participations에 3개 챌린저 요약
        List<SearchMemberItemV2Info> content = result.page().getContent();
        assertThat(content).hasSize(1);
        assertThat(content.get(0).memberId()).isEqualTo(10L);
        assertThat(content.get(0).participations()).hasSize(3);
        // primaryChallenger는 활성 기수(8L) 챌린저인 103번
        assertThat(content.get(0).primaryChallenger().challengerId()).isEqualTo(103L);
        assertThat(content.get(0).primaryChallenger().gisuId()).isEqualTo(8L);
    }

    @Test
    void 활성_기수_챌린저가_없으면_가장_최신_기수_챌린저가_대표로_선택된다() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> memberIdPage = new PageImpl<>(List.of(10L), pageable, 1);

        given(searchMemberPort.searchMemberIds(any(), any())).willReturn(memberIdPage);
        given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of(10L, profile(10L, "졸업자")));
        given(getChallengerUseCase.getAllBasicByMemberIds(anySet())).willReturn(Map.of(
            10L, List.of(
                challenger(101L, 10L, 6L, ChallengerPart.WEB, ChallengerStatus.GRADUATED),
                challenger(102L, 10L, 7L, ChallengerPart.SPRINGBOOT, ChallengerStatus.GRADUATED)
            )
        ));
        // 활성 기수가 9L이지만 이 회원은 9L 챌린저가 없음 → 활성 기수 챌린저 부재로 운영진 조회가 호출되지 않음
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisu(9L, 10L, true)));
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(
            gisu(6L, 7L, false),
            gisu(7L, 8L, false)
        ));

        SearchMemberV2Result result = memberSearchService.searchByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        SearchMemberItemV2Info item = result.page().getContent().get(0);
        // 가장 큰 generation(8) 챌린저가 대표 = 102L
        assertThat(item.primaryChallenger().challengerId()).isEqualTo(102L);
        assertThat(item.primaryChallenger().generation()).isEqualTo(8L);
        assertThat(item.isAdminInActiveGisu()).isFalse();
    }

    @Test
    void 활성_기수_챌린저가_운영진이면_isAdminInActiveGisu가_true다() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> memberIdPage = new PageImpl<>(List.of(10L), pageable, 1);

        given(searchMemberPort.searchMemberIds(any(), any())).willReturn(memberIdPage);
        given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of(10L, profile(10L, "운영진")));
        given(getChallengerUseCase.getAllBasicByMemberIds(anySet())).willReturn(Map.of(
            10L, List.of(
                challenger(101L, 10L, 8L, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE)
            )
        ));
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisu(8L, 9L, true)));
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(gisu(8L, 9L, true)));
        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet()))
            .willReturn(Map.of(101L, List.of(ChallengerRoleType.SCHOOL_PRESIDENT)));

        SearchMemberV2Result result = memberSearchService.searchByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        assertThat(result.page().getContent().get(0).isAdminInActiveGisu()).isTrue();
    }

    @Test
    void 휴지기에는_isAdminInActiveGisu가_항상_false이다() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> memberIdPage = new PageImpl<>(List.of(10L), pageable, 1);

        given(searchMemberPort.searchMemberIds(any(), any())).willReturn(memberIdPage);
        given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of(10L, profile(10L, "회원")));
        given(getChallengerUseCase.getAllBasicByMemberIds(anySet())).willReturn(Map.of(
            10L, List.of(challenger(101L, 10L, 7L, ChallengerPart.WEB, ChallengerStatus.GRADUATED))
        ));
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.empty()); // 휴지기
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(gisu(7L, 8L, false)));

        SearchMemberV2Result result = memberSearchService.searchByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        assertThat(result.page().getContent().get(0).isAdminInActiveGisu()).isFalse();
    }

    @Test
    void 검색결과가_비어있으면_빈_페이지를_반환한다() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(searchMemberPort.searchMemberIds(any(), any())).willReturn(emptyPage);

        SearchMemberV2Result result = memberSearchService.searchByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        assertThat(result.page().getContent()).isEmpty();
        assertThat(result.page().getTotalElements()).isZero();
    }
}
