package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewGraphQlResponse;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationReviewBatchResolverTest {

    @Mock
    SearchRecruitingApplicationUseCase searchApplicationUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    RecruitingGraphQlPermissionSupport permissionSupport;

    @InjectMocks
    RecruitingApplicationReviewGraphQlController sut;

    @Test
    @DisplayName("review nested field는 접근 가능한 지원서를 한 번에 조회한다")
    void review_nested_field는_접근_가능한_지원서를_한_번에_조회한다() {
        RecruitingApplicationGraphQlResponse reviewable = application(1L, true);
        RecruitingApplicationGraphQlResponse hidden = application(2L, false);
        given(permissionSupport.currentMemberId()).willReturn(99L);
        given(searchApplicationUseCase.getDetails(Set.of(1L), 99L)).willReturn(Map.of(1L, detail(1L)));

        Map<RecruitingApplicationGraphQlResponse, RecruitingApplicationReviewGraphQlResponse> result =
            sut.reviews(List.of(reviewable, hidden));

        assertThat(result.get(reviewable).applicationId()).isEqualTo(1L);
        assertThat(result.get(hidden)).isNull();
        then(searchApplicationUseCase).should().getDetails(Set.of(1L), 99L);
    }

    @Test
    @DisplayName("익명 지원서 review의 applicant는 null로 유지한다")
    void 익명_지원서_review의_applicant는_null로_유지한다() {
        RecruitingApplicationReviewGraphQlResponse anonymous = RecruitingApplicationReviewGraphQlResponse.from(
            detail(1L)
        );
        given(getMemberUseCase.findAllByIds(Set.of())).willReturn(Map.of());

        assertThat(sut.applicants(List.of(anonymous)).get(anonymous)).isNull();
    }

    private static RecruitingApplicationGraphQlResponse application(Long id, boolean reviewAccess) {
        return new RecruitingApplicationGraphQlResponse(
            id,
            10L,
            20L,
            RecruitingApplicationStatus.SUBMITTED,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            ChallengerTrack.PLAN,
            null,
            null,
            false,
            reviewAccess,
            null,
            null
        );
    }

    private static RecruitingApplicationDetailInfo detail(Long id) {
        return RecruitingApplicationDetailInfo.builder()
            .application(RecruitingApplicationSummaryInfo.builder()
                .applicationId(id)
                .applicantName("지원자")
                .email("applicant@example.com")
                .applicantMemberId(null)
                .firstChoice(ChallengerTrack.PLAN)
                .status(RecruitingApplicationStatus.SUBMITTED)
                .registrationStatus(RecruitingApplicationRegistrationStatus.NOT_READY)
                .build())
            .formResponseId(100L)
            .answers(List.of())
            .build();
    }
}
