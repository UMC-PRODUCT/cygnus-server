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
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.CheckChallengerHistoryUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.ChallengerSearchItemV2Info;
import com.umc.product.member.application.port.in.query.dto.ChallengerSearchV2Result;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.out.SearchMemberPort;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSearchService.searchChallengersByV2 — 챌린저 단위 v2 검색")
class ChallengerSearchV2Test {

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

    private Challenger challenger(Long id, Long memberId, ChallengerPart part, Long gisuId) {
        Challenger c = Challenger.builder()
            .memberId(memberId)
            .part(part)
            .gisuId(gisuId)
            .build();
        ReflectionTestUtils.setField(c, "id", id);
        return c;
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

    private GisuInfo gisu(Long gisuId, Long generation, boolean active) {
        return new GisuInfo(gisuId, generation, Instant.now(), Instant.now().plusSeconds(100), active);
    }

    private ChallengerBasicInfo basic(Long id, Long memberId, ChallengerPart part, Long gisuId) {
        return new ChallengerBasicInfo(id, memberId, gisuId, part, ChallengerStatus.ACTIVE);
    }

    @Test
    void 챌린저_단위_검색은_같은_회원이라도_여러_row로_반환된다() {
        Pageable pageable = PageRequest.of(0, 10);
        Challenger c1 = challenger(1L, 10L, ChallengerPart.WEB, 6L);
        Challenger c2 = challenger(2L, 10L, ChallengerPart.SPRINGBOOT, 7L);
        Challenger c3 = challenger(3L, 10L, ChallengerPart.SPRINGBOOT, 8L);
        Page<Challenger> page = new PageImpl<>(List.of(c1, c2, c3), pageable, 3);

        given(searchMemberPort.search(any(), any())).willReturn(page);
        given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of(10L, profile(10L, "홍길동")));
        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(
            gisu(6L, 7L, false), gisu(7L, 8L, false), gisu(8L, 9L, true)
        ));
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.empty());

        ChallengerSearchV2Result result = memberSearchService.searchChallengersByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        List<ChallengerSearchItemV2Info> content = result.page().getContent();
        // 같은 회원이지만 챌린저 3개라 3개 row
        assertThat(content).hasSize(3);
        assertThat(content).extracting(ChallengerSearchItemV2Info::memberId)
            .containsExactly(10L, 10L, 10L);
        assertThat(content).extracting(ChallengerSearchItemV2Info::challengerId)
            .containsExactly(1L, 2L, 3L);
    }

    @Test
    void challengerStatus와_isAdminInActiveGisu가_매핑된다() {
        Pageable pageable = PageRequest.of(0, 10);
        Long activeGisuId = 8L;
        Challenger activeRow = challenger(1L, 10L, ChallengerPart.SPRINGBOOT, activeGisuId);
        Page<Challenger> page = new PageImpl<>(List.of(activeRow), pageable, 1);

        given(searchMemberPort.search(any(), any())).willReturn(page);
        given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of(10L, profile(10L, "운영진")));
        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of(
            1L, List.of(ChallengerRoleType.SCHOOL_PRESIDENT)
        ));
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(gisu(activeGisuId, 9L, true)));
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisu(activeGisuId, 9L, true)));
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(anySet(), any())).willReturn(List.of(
            basic(1L, 10L, ChallengerPart.SPRINGBOOT, activeGisuId)
        ));

        ChallengerSearchV2Result result = memberSearchService.searchChallengersByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        ChallengerSearchItemV2Info item = result.page().getContent().get(0);
        assertThat(item.challengerStatus()).isEqualTo(ChallengerStatus.ACTIVE);
        assertThat(item.roleTypes()).containsExactly(ChallengerRoleType.SCHOOL_PRESIDENT);
        assertThat(item.isAdminInActiveGisu()).isTrue();
    }

    @Test
    void 같은_회원의_과거_기수_행도_활성기수_운영진이면_isAdminInActiveGisu_true이다() {
        Pageable pageable = PageRequest.of(0, 10);
        Long activeGisuId = 8L;
        Challenger past = challenger(1L, 10L, ChallengerPart.WEB, 7L);
        Challenger active = challenger(2L, 10L, ChallengerPart.SPRINGBOOT, activeGisuId);
        Page<Challenger> page = new PageImpl<>(List.of(past, active), pageable, 2);

        given(searchMemberPort.search(any(), any())).willReturn(page);
        given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of(10L, profile(10L, "운영진")));
        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of(
            2L, List.of(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER)
        ));
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(
            gisu(7L, 8L, false), gisu(activeGisuId, 9L, true)
        ));
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisu(activeGisuId, 9L, true)));
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(anySet(), any())).willReturn(List.of(
            basic(2L, 10L, ChallengerPart.SPRINGBOOT, activeGisuId)
        ));

        ChallengerSearchV2Result result = memberSearchService.searchChallengersByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        List<ChallengerSearchItemV2Info> content = result.page().getContent();
        // 두 행 모두 isAdminInActiveGisu=true (회원 단위로 확장)
        assertThat(content).extracting(ChallengerSearchItemV2Info::isAdminInActiveGisu)
            .containsExactly(true, true);
    }

    @Test
    void 활성기수_행이_현재_페이지에_없어도_활성기수_운영진이면_isAdminInActiveGisu_true이다() {
        Pageable pageable = PageRequest.of(0, 10);
        Long activeGisuId = 8L;
        Challenger past = challenger(1L, 10L, ChallengerPart.WEB, 7L);
        Page<Challenger> page = new PageImpl<>(List.of(past), pageable, 1);

        given(searchMemberPort.search(any(), any())).willReturn(page);
        given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of(10L, profile(10L, "운영진")));
        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet()))
            .willReturn(Map.of())
            .willReturn(Map.of(2L, List.of(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER)));
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(gisu(7L, 8L, false)));
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisu(activeGisuId, 9L, true)));
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(anySet(), any())).willReturn(List.of(
            basic(2L, 10L, ChallengerPart.SPRINGBOOT, activeGisuId)
        ));

        ChallengerSearchV2Result result = memberSearchService.searchChallengersByV2(
            new SearchMemberQuery(null, null, null, null, null), REQUESTER_MEMBER_ID, pageable
        );

        assertThat(result.page().getContent().get(0).isAdminInActiveGisu()).isTrue();
    }
}
