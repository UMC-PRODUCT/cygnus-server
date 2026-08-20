package com.umc.product.demoday.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.query.participant.GuestDemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.project.application.port.in.query.ListProjectParticipationUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("데모데이 투표 대상 검증기")
class DemodayVoteTargetValidatorTest {

    private static final Long POLL_ID = 1L;
    private static final Long MEMBER_ID = 10L;
    private static final Long OWN_PROJECT_ID = 20L;
    private static final Long OTHER_PROJECT_ID = 30L;
    private static final int OWN_BOOTH_CODE = 11;
    private static final int OTHER_BOOTH_CODE = 12;
    private static final int EXTERNAL_BOOTH_CODE = 13;

    @Mock
    private ListProjectParticipationUseCase listProjectParticipationUseCase;

    @InjectMocks
    private DemodayVoteTargetValidator validator;

    @Test
    @DisplayName("회원의 소속 프로젝트 부스는 투표 대상 목록에서 제외한다")
    void filterOwnProjectBooth() {
        // given
        DemodayBooth ownBooth = DemodayBooth.forProject(POLL_ID, OWN_BOOTH_CODE, OWN_PROJECT_ID);
        DemodayBooth otherBooth = DemodayBooth.forProject(POLL_ID, OTHER_BOOTH_CODE, OTHER_PROJECT_ID);
        DemodayBooth externalBooth = DemodayBooth.forExternal(POLL_ID, EXTERNAL_BOOTH_CODE, "외부 부스");
        given(listProjectParticipationUseCase.listParticipatingProjectIds(
            Set.of(OWN_PROJECT_ID, OTHER_PROJECT_ID), MEMBER_ID))
            .willReturn(Set.of(OWN_PROJECT_ID));

        // when
        List<DemodayBooth> result = validator.filterEligibleBooths(
            List.of(ownBooth, otherBooth, externalBooth),
            new MemberDemodayParticipant(MEMBER_ID)
        );

        // then
        assertThat(result).containsExactly(otherBooth, externalBooth);
    }

    @Test
    @DisplayName("게스트에게는 모든 부스를 투표 대상으로 제공한다")
    void keepAllBoothsForGuest() {
        // given
        List<DemodayBooth> booths = List.of(
            DemodayBooth.forProject(POLL_ID, OWN_BOOTH_CODE, OWN_PROJECT_ID),
            DemodayBooth.forExternal(POLL_ID, EXTERNAL_BOOTH_CODE, "외부 부스")
        );

        // when
        List<DemodayBooth> result = validator.filterEligibleBooths(
            booths,
            new GuestDemodayParticipant(100L)
        );

        // then
        assertThat(result).containsExactlyElementsOf(booths);
        verifyNoInteractions(listProjectParticipationUseCase);
    }

    @Test
    @DisplayName("회원이 소속 프로젝트 부스를 선택하면 투표를 거부한다")
    void rejectOwnProjectBooth() {
        // given
        DemodayBooth ownBooth = DemodayBooth.forProject(POLL_ID, OWN_BOOTH_CODE, OWN_PROJECT_ID);
        given(listProjectParticipationUseCase.listParticipatingProjectIds(
            Set.of(OWN_PROJECT_ID), MEMBER_ID))
            .willReturn(Set.of(OWN_PROJECT_ID));

        // when & then
        assertThatThrownBy(() -> validator.validateEligibleBooth(
            ownBooth,
            new MemberDemodayParticipant(MEMBER_ID)
        )).isInstanceOfSatisfying(DemodayDomainException.class, exception ->
            assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_OWN_BOOTH_FORBIDDEN));
    }

    @Test
    @DisplayName("외부 부스는 프로젝트 소속 확인 없이 투표할 수 있다")
    void allowExternalBooth() {
        // given
        DemodayBooth externalBooth = DemodayBooth.forExternal(POLL_ID, EXTERNAL_BOOTH_CODE, "외부 부스");

        // when
        validator.validateEligibleBooth(externalBooth, new MemberDemodayParticipant(MEMBER_ID));

        // then
        verifyNoInteractions(listProjectParticipationUseCase);
    }
}
