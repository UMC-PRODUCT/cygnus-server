package com.umc.product.organization.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.organization.adapter.in.web.dto.request.AssignSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateChapterRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateStudyGroupRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateStudyGroupScheduleRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UnassignSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateStudyGroupRequest;
import com.umc.product.organization.application.port.in.command.CreateStudyGroupScheduleUseCase;
import com.umc.product.organization.application.port.in.command.ManageChapterUseCase;
import com.umc.product.organization.application.port.in.command.ManageSchoolUseCase;
import com.umc.product.organization.application.port.in.command.ManageStudyGroupUseCase;
import com.umc.product.organization.application.port.in.command.ManageUmcProductChapterUseCase;
import com.umc.product.organization.application.port.in.command.ManageUmcProductMemberUseCase;
import com.umc.product.organization.application.port.in.command.ManageUmcProductSquadUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.UnassignedSchoolInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupWithMemberAndMentorInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("Organization Controller 잔여 정상·인증 경로")
class OrganizationControllerResidualTest {

    @Mock
    ManageStudyGroupUseCase manageStudyGroupUseCase;
    @Mock
    GetStudyGroupUseCase getStudyGroupUseCase;
    @Mock
    ManageChapterUseCase manageChapterUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    ManageSchoolUseCase manageSchoolUseCase;
    @Mock
    GetSchoolUseCase getSchoolUseCase;
    @Mock
    CreateStudyGroupScheduleUseCase createStudyGroupScheduleUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    ManageUmcProductChapterUseCase manageUmcProductChapterUseCase;
    @Mock
    ManageUmcProductMemberUseCase manageUmcProductMemberUseCase;
    @Mock
    ManageUmcProductSquadUseCase manageUmcProductSquadUseCase;

    @Test
    @DisplayName("스터디 그룹 command controller는 생성부터 삭제까지 모든 명령을 변환한다")
    void 스터디_그룹_명령을_전달한다() {
        var controller = new StudyGroupCommandController(manageStudyGroupUseCase);

        controller.create(new CreateStudyGroupRequest(
            "스터디", 1L, ChallengerPart.SPRINGBOOT, Set.of(2L), Set.of(3L)
        ));
        controller.update(4L, new UpdateStudyGroupRequest("변경"));
        controller.addMember(4L, 5L);
        controller.addMentor(4L, 6L);
        controller.deleteMember(4L, 5L);
        controller.deleteMentor(4L, 6L);
        controller.delete(4L);

        then(manageStudyGroupUseCase).should().create(any());
        then(manageStudyGroupUseCase).should().update(any());
        then(manageStudyGroupUseCase).should().addMember(any());
        then(manageStudyGroupUseCase).should().addMentor(any());
        then(manageStudyGroupUseCase).should().deleteMember(any());
        then(manageStudyGroupUseCase).should().deleteMentor(any());
        then(manageStudyGroupUseCase).should().delete(4L);
    }

    @Test
    @DisplayName("스터디 그룹 query controller는 커서 초과분과 단건 상세를 응답으로 변환한다")
    void 스터디_그룹을_조회한다() {
        var controller = new StudyGroupQueryController(getStudyGroupUseCase);
        var first = groupInfo(1L);
        var second = groupInfo(2L);
        given(getStudyGroupUseCase.getMyStudyGroups(10L, null, 1)).willReturn(List.of(first, second));
        given(getStudyGroupUseCase.getWithMemberAndMentorInfoById(1L)).willReturn(first);

        var cursor = controller.getStudyGroups(MemberPrincipal.builder().memberId(10L).build(), null, 1);

        assertThat(cursor.content()).hasSize(1);
        assertThat(cursor.hasNext()).isTrue();
        assertThat(cursor.nextCursor()).isEqualTo(1L);
        assertThat(controller.getStudyGroupInfo(1L).studyGroupId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("지부 controller는 bulk 생성·삭제·단건·학교 포함 조회를 수행한다")
    void 지부를_명령하고_조회한다() {
        var commandController = new ChapterCommandController(manageChapterUseCase);
        var queryController = new ChapterQueryController(getChapterUseCase);
        var request = new CreateChapterRequest(1L, "서울", List.of(2L));
        given(manageChapterUseCase.create(any())).willReturn(10L, 20L);
        given(getChapterUseCase.getChapterById(10L)).willReturn(new ChapterInfo(10L, "서울"));
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(1L)).willReturn(List.of(
            new ChapterWithSchoolsInfo(10L, "서울", List.of())
        ));

        assertThat(commandController.createChapterBulk(List.of(request, request))).containsExactly(10L, 20L);
        commandController.deleteChapter(10L);
        assertThat(queryController.getChapterById(10L).id()).isEqualTo(10L);
        assertThat(queryController.getChaptersWithSchoolsByGisuId(1L).chapters()).hasSize(1);
    }

    @Test
    @DisplayName("학교 controller는 지부 배정·해제 및 기수별·미배정 조회를 수행한다")
    void 학교를_명령하고_조회한다() {
        var commandController = new SchoolCommandController(manageSchoolUseCase);
        var queryController = new SchoolQueryController(getSchoolUseCase);
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(new SchoolDetailInfo(
            2L, "서울", "학교", 3L, null, null, List.of(), true, null, null
        )));
        given(getSchoolUseCase.getUnassignedSchools(1L)).willReturn(List.of(new UnassignedSchoolInfo(3L, "학교")));

        commandController.assignToChapter(3L, new AssignSchoolRequest(2L));
        commandController.unassignFromChapter(3L, new UnassignSchoolRequest(1L));
        assertThat(queryController.getSchoolListsByGisu(1L)).hasSize(1);
        assertThat(queryController.getUnassignedSchools(1L).schools()).hasSize(1);
    }

    @Test
    @DisplayName("스터디 일정과 기수 단건 controller는 use case 결과를 변환한다")
    void 일정과_기수를_조회한다() {
        var scheduleController = new StudyGroupScheduleController(createStudyGroupScheduleUseCase);
        var gisuController = new GisuQueryController(getGisuUseCase);
        given(createStudyGroupScheduleUseCase.create(any())).willReturn(5L);
        given(getGisuUseCase.getById(1L)).willReturn(new GisuInfo(
            1L, 9L, Instant.EPOCH, Instant.EPOCH, true
        ));

        assertThat(scheduleController.create(new CreateStudyGroupScheduleRequest(1L, 2L, 3L))).isEqualTo(5L);
        assertThat(gisuController.getGisu(1L).gisuId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("UMC PRODUCT command controller는 인증 주체가 없으면 요청을 거부한다")
    void UMC_PRODUCT_인증_주체를_검증한다() {
        assertThatThrownBy(() -> new UmcProductChapterCommandController(manageUmcProductChapterUseCase)
            .delete(1L, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> new UmcProductMemberCommandController(manageUmcProductMemberUseCase)
            .delete(1L, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> new UmcProductSquadCommandController(manageUmcProductSquadUseCase)
            .delete(1L, null)).isInstanceOf(AccessDeniedException.class);
    }

    private StudyGroupWithMemberAndMentorInfo groupInfo(Long id) {
        return StudyGroupWithMemberAndMentorInfo.create(
            id, "스터디-" + id, 1L, ChallengerPart.SPRINGBOOT,
            Instant.EPOCH, List.of(), List.of()
        );
    }
}
