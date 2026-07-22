package com.umc.product.analytics.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.analytics.domain.AdminAnalyticsRoleType;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.analytics.domain.AnalyticsDomainException;
import com.umc.product.analytics.domain.AnalyticsErrorCode;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAnalyticsScopeResolver")
class AdminAnalyticsScopeResolverTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 7L;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @InjectMocks
    AdminAnalyticsScopeResolver sut;

    @Test
    @DisplayName("SUPER_ADMIN은 member system role로 중앙 스코프를 얻는다")
    void SUPER_ADMIN은_member_system_role로_중앙_스코프를_얻는다() {
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);

        AdminAnalyticsScope scope = sut.resolve(MEMBER_ID, GISU_ID, 10L, 20L, ChallengerPart.SPRINGBOOT);

        assertThat(scope.type()).isEqualTo(AdminAnalyticsScopeType.CENTRAL);
        assertThat(scope.gisuId()).isEqualTo(GISU_ID);
        assertThat(scope.chapterId()).isEqualTo(10L);
        assertThat(scope.schoolId()).isEqualTo(20L);
        assertThat(scope.responsiblePart()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(scope.roleType()).isEqualTo(AdminAnalyticsRoleType.SUPER_ADMIN);
    }

    @Test
    @DisplayName("중앙 운영진은 요청한 지부와 학교 필터를 그대로 사용한다")
    void 중앙_운영진은_요청한_지부와_학교_필터를_그대로_사용한다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, null)));

        AdminAnalyticsScope scope = sut.resolve(MEMBER_ID, GISU_ID, 10L, 20L, ChallengerPart.SPRINGBOOT);

        assertThat(scope.type()).isEqualTo(AdminAnalyticsScopeType.CENTRAL);
        assertThat(scope.gisuId()).isEqualTo(GISU_ID);
        assertThat(scope.chapterId()).isEqualTo(10L);
        assertThat(scope.schoolId()).isEqualTo(20L);
        assertThat(scope.responsiblePart()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(scope.roleType()).isEqualTo(AdminAnalyticsRoleType.CENTRAL_PRESIDENT);
    }

    @Test
    @DisplayName("지부장은 다른 지부를 요청하면 거부된다")
    void 지부장은_다른_지부를_요청하면_거부된다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, null)));

        assertThatThrownBy(() -> sut.resolve(MEMBER_ID, GISU_ID, 99L, null, null))
            .isInstanceOf(AnalyticsDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AnalyticsErrorCode.RESOURCE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("학교 운영진은 학교 필터가 없어도 본인 학교로 스코프가 고정된다")
    void 학교_운영진은_학교_필터가_없어도_본인_학교로_스코프가_고정된다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 30L, null)));

        AdminAnalyticsScope scope = sut.resolve(MEMBER_ID, GISU_ID, null, null, null);

        assertThat(scope.type()).isEqualTo(AdminAnalyticsScopeType.SCHOOL);
        assertThat(scope.schoolId()).isEqualTo(30L);
        assertThat(scope.chapterId()).isNull();
        assertThat(scope.responsiblePart()).isNull();
        assertThat(scope.roleType()).isEqualTo(AdminAnalyticsRoleType.SCHOOL_PRESIDENT);
    }

    @Test
    @DisplayName("학교 파트장은 담당 파트까지 스코프에 포함된다")
    void 학교_파트장은_담당_파트까지_스코프에_포함된다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(
                role(ChallengerRoleType.SCHOOL_PART_LEADER, OrganizationType.SCHOOL, 30L, ChallengerPart.ANDROID)
            ));

        AdminAnalyticsScope scope = sut.resolve(MEMBER_ID, GISU_ID, null, null, null);

        assertThat(scope.type()).isEqualTo(AdminAnalyticsScopeType.SCHOOL_PART);
        assertThat(scope.schoolId()).isEqualTo(30L);
        assertThat(scope.responsiblePart()).isEqualTo(ChallengerPart.ANDROID);
        assertThat(scope.roleType()).isEqualTo(AdminAnalyticsRoleType.SCHOOL_PART_LEADER);
    }

    @Test
    @DisplayName("운영진 역할이 없으면 대시보드 접근이 거부된다")
    void 운영진_역할이_없으면_대시보드_접근이_거부된다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of());

        assertThatThrownBy(() -> sut.resolve(MEMBER_ID, GISU_ID, null, null, null))
            .isInstanceOf(AnalyticsDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AnalyticsErrorCode.RESOURCE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("기수를 생략하면 활성 기수를 사용하고 지부장은 본인 지부 스코프를 얻는다")
    void 활성_기수와_지부장_스코프를_해석한다() {
        given(getGisuUseCase.getActiveGisuId()).willReturn(GISU_ID);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, null)));

        AdminAnalyticsScope scope = sut.resolve(MEMBER_ID, null);

        assertThat(scope.type()).isEqualTo(AdminAnalyticsScopeType.CHAPTER);
        assertThat(scope.chapterId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("학교 운영진의 다른 학교 요청과 파트장의 다른 파트 요청은 거부한다")
    void 학교와_파트_scope_우회를_거부한다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 30L, null)));
        assertThatThrownBy(() -> sut.resolve(MEMBER_ID, GISU_ID, null, 31L, null))
            .isInstanceOf(AnalyticsDomainException.class);

        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(
                role(ChallengerRoleType.SCHOOL_PART_LEADER, OrganizationType.SCHOOL, 30L, ChallengerPart.ANDROID)
            ));
        assertThatThrownBy(() -> sut.resolve(MEMBER_ID, GISU_ID, null, 30L, ChallengerPart.WEB))
            .isInstanceOf(AnalyticsDomainException.class);
    }

    @Test
    @DisplayName("다른 기수 역할만 있으면 접근을 거부한다")
    void 다른_기수_역할을_제외한다() {
        ChallengerRoleInfo otherGisu = ChallengerRoleInfo.builder()
            .roleType(ChallengerRoleType.CENTRAL_PRESIDENT)
            .gisuId(GISU_ID + 1)
            .build();
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(otherGisu));

        assertThatThrownBy(() -> sut.resolve(MEMBER_ID, GISU_ID))
            .isInstanceOf(AnalyticsDomainException.class);
    }

    @Test
    @DisplayName("여러 역할은 중앙·지부·학교·파트장 우선순위로 정렬한다")
    void 여러_역할에서_최상위_역할을_선택한다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, OrganizationType.SCHOOL, 30L, ChallengerPart.WEB),
            role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 30L, null),
            role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, null),
            role(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, OrganizationType.CENTRAL, null, null)
        ));

        assertThat(sut.resolve(MEMBER_ID, GISU_ID).type()).isEqualTo(AdminAnalyticsScopeType.CENTRAL);
    }

    private ChallengerRoleInfo role(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId,
        ChallengerPart responsiblePart
    ) {
        return ChallengerRoleInfo.builder()
            .id(1L)
            .challengerId(100L)
            .roleType(roleType)
            .organizationType(organizationType)
            .organizationId(organizationId)
            .responsiblePart(responsiblePart)
            .gisuId(GISU_ID)
            .build();
    }
}
