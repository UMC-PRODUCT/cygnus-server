package com.umc.product.inquiry.application.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.inquiry.application.access.InquiryAccessScope.OperatorScoped;
import com.umc.product.inquiry.application.access.InquiryAccessScope.OwnedOnly;
import com.umc.product.inquiry.application.access.InquiryAccessScope.TargetCondition;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryAccessScopeResolver")
class InquiryAccessScopeResolverTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 10L;
    private static final Long CHAPTER_ORG_ID = 20L;
    private static final Long SCHOOL_ORG_ID = 30L;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @InjectMocks
    InquiryAccessScopeResolver sut;

    @Test
    @DisplayName("역할 목록이 비어 있으면 OwnedOnly가 반환된다")
    void 역할_목록이_비어_있으면_OwnedOnly_반환() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of());

        InquiryAccessScope scope = sut.resolve(MEMBER_ID);

        assertThat(scope).isInstanceOf(OwnedOnly.class);
        assertThat(((OwnedOnly) scope).memberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("isAtLeastCentralMember가 true인 역할이 있으면 CENTRAL과 PRODUCT_TEAM conditions 2개가 생성된다")
    void 중앙멤버_역할이면_CENTRAL_PRODUCT_TEAM_조건_2개() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, OrganizationType.CENTRAL, null)));

        InquiryAccessScope scope = sut.resolve(MEMBER_ID);

        assertThat(scope).isInstanceOf(OperatorScoped.class);
        List<TargetCondition> conditions = ((OperatorScoped) scope).conditions();
        assertThat(conditions).hasSize(2);
        assertThat(conditions).extracting(TargetCondition::target)
            .containsExactlyInAnyOrder(InquiryTarget.CENTRAL, InquiryTarget.PRODUCT_TEAM);
        assertThat(conditions).allSatisfy(c -> {
            assertThat(c.organizationId()).isNull();
            assertThat(c.gisuId()).isEqualTo(GISU_ID);
        });
    }

    @Test
    @DisplayName("CHAPTER 역할이 있으면 CHAPTER target의 condition이 생성된다")
    void CHAPTER_역할이면_CHAPTER_target_condition_생성() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, CHAPTER_ORG_ID)));

        InquiryAccessScope scope = sut.resolve(MEMBER_ID);

        assertThat(scope).isInstanceOf(OperatorScoped.class);
        List<TargetCondition> conditions = ((OperatorScoped) scope).conditions();
        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).target()).isEqualTo(InquiryTarget.CHAPTER);
        assertThat(conditions.get(0).organizationId()).isEqualTo(CHAPTER_ORG_ID);
        assertThat(conditions.get(0).gisuId()).isEqualTo(GISU_ID);
    }

    @Test
    @DisplayName("SCHOOL 역할이 있으면 SCHOOL target의 condition이 생성된다")
    void SCHOOL_역할이면_SCHOOL_target_condition_생성() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ORG_ID)));

        InquiryAccessScope scope = sut.resolve(MEMBER_ID);

        assertThat(scope).isInstanceOf(OperatorScoped.class);
        List<TargetCondition> conditions = ((OperatorScoped) scope).conditions();
        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).target()).isEqualTo(InquiryTarget.SCHOOL);
        assertThat(conditions.get(0).organizationId()).isEqualTo(SCHOOL_ORG_ID);
        assertThat(conditions.get(0).gisuId()).isEqualTo(GISU_ID);
    }

    @Test
    @DisplayName("CENTRAL + CHAPTER 역할을 동시에 보유하면 conditions가 3개 생성된다")
    void CENTRAL_CHAPTER_복수_역할이면_conditions_3개() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(
                role(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null),
                role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, CHAPTER_ORG_ID)
            ));

        InquiryAccessScope scope = sut.resolve(MEMBER_ID);

        // CENTRAL 역할 → CENTRAL + PRODUCT_TEAM (2개), CHAPTER 역할 → CHAPTER (1개)
        assertThat(scope).isInstanceOf(OperatorScoped.class);
        List<TargetCondition> conditions = ((OperatorScoped) scope).conditions();
        assertThat(conditions).hasSize(3);
        assertThat(conditions).extracting(TargetCondition::target)
            .containsExactlyInAnyOrder(
                InquiryTarget.CENTRAL,
                InquiryTarget.PRODUCT_TEAM,
                InquiryTarget.CHAPTER
            );
    }

    @Test
    @DisplayName("운영진이어도 OperatorScoped.memberId()는 본인 memberId를 보존한다")
    void 운영진이어도_OperatorScoped에_본인_memberId가_보존된다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, OrganizationType.CENTRAL, null)));

        InquiryAccessScope scope = sut.resolve(MEMBER_ID);

        assertThat(scope).isInstanceOf(OperatorScoped.class);
        assertThat(((OperatorScoped) scope).memberId()).isEqualTo(MEMBER_ID);
    }

    private ChallengerRoleInfo role(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId
    ) {
        return ChallengerRoleInfo.builder()
            .id(1L)
            .challengerId(100L)
            .roleType(roleType)
            .organizationType(organizationType)
            .organizationId(organizationId)
            .gisuId(GISU_ID)
            .build();
    }
}
