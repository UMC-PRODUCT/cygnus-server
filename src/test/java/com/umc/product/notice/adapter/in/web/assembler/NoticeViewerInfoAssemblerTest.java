package com.umc.product.notice.adapter.in.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeViewerInfoAssembler")
class NoticeViewerInfoAssemblerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 9L;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChapterUseCase getChapterUseCase;

    @InjectMocks
    NoticeViewerInfoAssembler sut;

    @Test
    @DisplayName("system role SUPER_ADMIN은 challenger role 없이 최상위 공지 역할을 얻는다")
    void system_super_admin_has_central_member_notice_role() {
        given(getChallengerUseCase.findByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(Optional.empty());
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);
        given(getMemberUseCase.findAllByIds(java.util.Set.of(MEMBER_ID))).willReturn(Map.of(
            MEMBER_ID,
            MemberInfo.builder().id(MEMBER_ID).build()
        ));

        NoticeViewerInfo result = sut.toMemberIdAndGisuId(MEMBER_ID, GISU_ID);

        assertThat(result.viewerRole()).isEqualTo(NoticeTab.CENTRAL_MEMBER);
        verifyNoInteractions(getChapterUseCase);
    }

    @Test
    @DisplayName("챌린저 파트와 담당 파트를 합치고 학교의 지부와 최상위 역할을 조립한다")
    void assembles_parts_chapter_and_highest_staff_role() {
        given(getChallengerUseCase.findByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(Optional.of(
            ChallengerInfo.builder().memberId(MEMBER_ID).gisuId(GISU_ID).part(ChallengerPart.SPRINGBOOT).build()
        ));
        given(getChallengerRoleUseCase.getAllResponsiblePartByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
            .willReturn(Set.of(ChallengerPart.WEB));
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, GISU_ID),
            role(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, GISU_ID),
            role(ChallengerRoleType.SCHOOL_PRESIDENT, GISU_ID + 1)
        ));
        given(getMemberUseCase.findAllByIds(Set.of(MEMBER_ID))).willReturn(Map.of(
            MEMBER_ID,
            MemberInfo.builder().id(MEMBER_ID).schoolId(30L).build()
        ));
        given(getChapterUseCase.byGisuAndSchool(GISU_ID, 30L)).willReturn(new ChapterInfo(40L, "동부"));

        NoticeViewerInfo result = sut.toMemberIdAndGisuId(MEMBER_ID, GISU_ID);

        assertThat(result.memberParts()).containsExactlyInAnyOrder(ChallengerPart.SPRINGBOOT, ChallengerPart.WEB);
        assertThat(result.schoolId()).isEqualTo(30L);
        assertThat(result.chapterId()).isEqualTo(40L);
        assertThat(result.viewerRole()).isEqualTo(NoticeTab.CENTRAL_MEMBER);
    }

    @Test
    @DisplayName("회원·기수가 없거나 소속 정보 조회가 실패하면 안전한 빈 조회자 정보를 반환한다")
    void returns_empty_information_for_missing_inputs_and_failed_chapter_lookup() {
        given(getMemberUseCase.findAllByIds(Set.of(MEMBER_ID))).willReturn(Map.of(
            MEMBER_ID,
            MemberInfo.builder().id(MEMBER_ID).schoolId(30L).build()
        ));
        given(getChapterUseCase.byGisuAndSchool(GISU_ID, 30L)).willThrow(new IllegalStateException("missing"));
        given(getChallengerUseCase.findByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(Optional.empty());
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            role(ChallengerRoleType.CHAPTER_PRESIDENT, GISU_ID)
        ));

        NoticeViewerInfo result = sut.toMemberIdAndGisuId(MEMBER_ID, GISU_ID);

        assertThat(result.memberParts()).isEmpty();
        assertThat(result.schoolId()).isEqualTo(30L);
        assertThat(result.chapterId()).isNull();
        assertThat(result.viewerRole()).isNull();
    }

    @Test
    @DisplayName("null 입력과 조회되지 않은 회원은 외부 소속 조회 없이 빈 정보를 반환한다")
    void handles_null_inputs_and_missing_member() {
        given(getMemberUseCase.findAllByIds(Set.of(MEMBER_ID))).willReturn(Map.of());

        NoticeViewerInfo missingMember = sut.toMemberIdAndGisuId(MEMBER_ID, GISU_ID);
        NoticeViewerInfo nullInput = sut.toMemberIdAndGisuId(null, null);

        assertThat(missingMember.schoolId()).isNull();
        assertThat(missingMember.chapterId()).isNull();
        assertThat(nullInput.memberParts()).isEmpty();
        assertThat(nullInput.viewerRole()).isNull();
    }

    private ChallengerRoleInfo role(ChallengerRoleType type, Long gisuId) {
        return ChallengerRoleInfo.builder().roleType(type).gisuId(gisuId).build();
    }
}
