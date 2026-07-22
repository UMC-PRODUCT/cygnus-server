package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.CheckChallengerHistoryUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info.PrimaryChallenger;
import com.umc.product.member.application.port.out.SearchMemberPort;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSearchService 잔여 빈 입력 계약")
class MemberSearchServiceResidualTest {

    @Mock SearchMemberPort searchMemberPort;
    @Mock GetMemberUseCase getMemberUseCase;
    @Mock CheckChallengerHistoryUseCase checkChallengerHistoryUseCase;
    @Mock GetChallengerUseCase getChallengerUseCase;
    @Mock GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock GetGisuUseCase getGisuUseCase;
    @Mock MemberSearchAccessScopeResolver memberSearchAccessScopeResolver;

    @InjectMocks MemberSearchService sut;

    @Test
    @DisplayName("활성 기수 챌린저 로딩은 빈 content와 null member ID를 빈 map으로 처리한다")
    void 활성_기수_챌린저의_빈_입력을_처리한다() {
        Map<Long, ChallengerBasicInfo> emptyContent = ReflectionTestUtils.invokeMethod(
            sut, "loadActiveGisuChallengerByMemberId", List.of(), 20L);
        Challenger missingMember = Challenger.builder()
            .memberId(1L)
            .part(ChallengerPart.SPRINGBOOT)
            .gisuId(20L)
            .build();
        ReflectionTestUtils.setField(missingMember, "memberId", null);
        Map<Long, ChallengerBasicInfo> nullMembers = ReflectionTestUtils.invokeMethod(
            sut, "loadActiveGisuChallengerByMemberId", List.of(missingMember), 20L);

        assertThat(emptyContent).isEmpty();
        assertThat(nullMembers).isEmpty();
    }

    @Test
    @DisplayName("기수 정보와 챌린저가 없으면 generation map과 대표 챌린저도 비어 있다")
    void v2_검색_빈_입력을_처리한다() {
        Map<Long, Long> generations = ReflectionTestUtils.invokeMethod(
            sut, "loadGenerationMap", Map.of());
        PrimaryChallenger primary = ReflectionTestUtils.invokeMethod(
            sut, "selectPrimaryChallenger", List.of(), Map.of(), null);

        assertThat(generations).isEmpty();
        assertThat(primary).isNull();
    }
}
