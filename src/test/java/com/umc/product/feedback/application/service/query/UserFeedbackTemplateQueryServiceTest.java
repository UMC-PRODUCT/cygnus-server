package com.umc.product.feedback.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
class UserFeedbackTemplateQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 10L;

    @Mock
    LoadUserFeedbackTemplatePort loadPort;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetFormUseCase getFormUseCase;

    @Test
    @DisplayName("활성 기수가 없으면 피드백 대상 분류를 수행하지 않는다")
    void 활성_기수가_없으면_피드백_대상_분류를_수행하지_않는다() {
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.empty());

        assertThat(sut().findTemplate(MEMBER_ID, UserFeedbackContext.APPLICATION_MONITORING)).isEmpty();
        verify(getChallengerRoleUseCase, never()).isCentralMemberInGisu(MEMBER_ID, GISU_ID);
    }

    @Test
    @DisplayName("현재 기수 중앙 운영진은 challenger 이력과 무관하게 ADMIN 템플릿을 조회한다")
    void 현재_기수_중앙_운영진은_ADMIN_템플릿을_조회한다() {
        givenActiveGisu(10L);
        given(getChallengerRoleUseCase.isCentralMemberInGisu(MEMBER_ID, GISU_ID)).willReturn(true);
        givenTemplate(UserFeedbackTargetType.ADMIN);

        var result = sut().findTemplate(MEMBER_ID, UserFeedbackContext.APPLICATION_MONITORING);

        assertThat(result).get().extracting("targetType").isEqualTo(UserFeedbackTargetType.ADMIN);
        verify(getChallengerUseCase, never()).getAllByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("현재 기수 challenger가 아니면 템플릿을 반환하지 않는다")
    void 현재_기수_challenger가_아니면_템플릿을_반환하지_않는다() {
        givenActiveGisu(10L);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(challenger(9L, ChallengerPart.WEB)));

        assertThat(sut().findTemplate(MEMBER_ID, UserFeedbackContext.MATCHING_COMPLETED)).isEmpty();
        verify(loadPort, never()).findByContextAndTargetType(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("이전 기수 이력이 있으면 EXPERIENCED_CHALLENGER로 분류한다")
    void 이전_기수_이력이_있으면_EXPERIENCED_CHALLENGER로_분류한다() {
        givenActiveGisu(10L);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(
            challenger(GISU_ID, ChallengerPart.WEB),
            challenger(9L, ChallengerPart.WEB)
        ));
        givenTemplate(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);

        assertThat(sut().findTemplate(MEMBER_ID, UserFeedbackContext.MATCHING_COMPLETED))
            .get().extracting("targetType")
            .isEqualTo(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);
    }

    @Test
    @DisplayName("10기 PM은 이전 기수 이력이 없어도 EXPERIENCED_CHALLENGER로 분류한다")
    void 십기_PM은_이전_기수_이력이_없어도_EXPERIENCED_CHALLENGER로_분류한다() {
        givenActiveGisu(10L);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID))
            .willReturn(List.of(challenger(GISU_ID, ChallengerPart.PLAN)));
        givenTemplate(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);

        assertThat(sut().findTemplate(MEMBER_ID, UserFeedbackContext.MATCHING_COMPLETED))
            .get().extracting("targetType")
            .isEqualTo(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);
    }

    @Test
    @DisplayName("10기 비PM 신규 challenger는 NEW_CHALLENGER로 분류한다")
    void 십기_비PM_신규_challenger는_NEW_CHALLENGER로_분류한다() {
        givenActiveGisu(10L);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID))
            .willReturn(List.of(challenger(GISU_ID, ChallengerPart.WEB)));
        givenTemplate(UserFeedbackTargetType.NEW_CHALLENGER);

        assertThat(sut().findTemplate(MEMBER_ID, UserFeedbackContext.MATCHING_COMPLETED))
            .get().extracting("targetType")
            .isEqualTo(UserFeedbackTargetType.NEW_CHALLENGER);
    }

    @Test
    @DisplayName("10기가 아닌 신규 challenger는 파트와 무관하게 NEW_CHALLENGER로 분류한다")
    void 십기가_아닌_신규_challenger는_NEW_CHALLENGER로_분류한다() {
        givenActiveGisu(11L);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID))
            .willReturn(List.of(challenger(GISU_ID, ChallengerPart.PLAN)));
        givenTemplate(UserFeedbackTargetType.NEW_CHALLENGER);

        assertThat(sut().findTemplate(MEMBER_ID, UserFeedbackContext.MATCHING_COMPLETED))
            .get().extracting("targetType")
            .isEqualTo(UserFeedbackTargetType.NEW_CHALLENGER);
    }

    @Test
    @DisplayName("분류에 맞는 활성 템플릿이 없으면 Form을 조회하지 않는다")
    void 분류에_맞는_활성_템플릿이_없으면_Form을_조회하지_않는다() {
        givenActiveGisu(10L);
        given(getChallengerRoleUseCase.isCentralMemberInGisu(MEMBER_ID, GISU_ID)).willReturn(true);
        given(loadPort.findByContextAndTargetType(
            UserFeedbackContext.APPLICATION_MONITORING,
            UserFeedbackTargetType.ADMIN
        )).willReturn(Optional.empty());

        assertThat(sut().findTemplate(MEMBER_ID, UserFeedbackContext.APPLICATION_MONITORING)).isEmpty();
        verify(getFormUseCase, never()).getFormWithStructure(org.mockito.ArgumentMatchers.anyLong());
    }

    private UserFeedbackTemplateQueryService sut() {
        return new UserFeedbackTemplateQueryService(
            loadPort,
            getGisuUseCase,
            getChallengerUseCase,
            getChallengerRoleUseCase,
            getFormUseCase
        );
    }

    private void givenActiveGisu(Long generation) {
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(new GisuInfo(
            GISU_ID,
            generation,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-08-31T00:00:00Z"),
            true
        )));
    }

    private void givenTemplate(UserFeedbackTargetType targetType) {
        UserFeedbackTemplate template = org.mockito.Mockito.mock(UserFeedbackTemplate.class);
        given(template.getId()).willReturn(100L);
        given(template.getContext()).willReturn(UserFeedbackContext.MATCHING_COMPLETED);
        given(template.getTargetType()).willReturn(targetType);
        given(template.getFormId()).willReturn(200L);
        given(loadPort.findByContextAndTargetType(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(targetType)))
            .willReturn(Optional.of(template));
        given(getFormUseCase.getFormWithStructure(200L)).willReturn(FormWithStructureInfo.builder()
            .formId(200L)
            .sections(List.of())
            .build());
    }

    private ChallengerInfo challenger(Long gisuId, ChallengerPart part) {
        return ChallengerInfo.builder()
            .challengerId(gisuId)
            .memberId(MEMBER_ID)
            .gisuId(gisuId)
            .part(part)
            .build();
    }
}
