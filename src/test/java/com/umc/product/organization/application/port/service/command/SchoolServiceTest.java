package com.umc.product.organization.application.port.service.command;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.dto.AssignSchoolCommand;
import com.umc.product.organization.application.port.in.command.dto.UnassignSchoolCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateSchoolCommand;
import com.umc.product.organization.application.port.out.command.SaveChapterSchoolPort;
import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchoolService")
class SchoolServiceTest {

    private static final Long SCHOOL_ID = 30L;
    private static final Long OTHER_SCHOOL_ID = 31L;
    private static final Long CHAPTER_ID = 40L;
    private static final Long GISU_ID = 9L;

    @Mock
    LoadChapterPort loadChapterPort;

    @Mock
    LoadSchoolPort loadSchoolPort;

    @Mock
    SaveSchoolPort saveSchoolPort;

    @Mock
    SaveChapterSchoolPort saveChapterSchoolPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @InjectMocks
    SchoolService sut;

    @Test
    @DisplayName("학교를 지부에 배정하면 해당 학교 회원들의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_assign_school_to_chapter() {
        given(loadSchoolPort.findSchoolDetailById(SCHOOL_ID)).willReturn(school());
        given(loadChapterPort.findById(CHAPTER_ID)).willReturn(chapter());
        given(getMemberUseCase.listIdsBySchoolId(SCHOOL_ID)).willReturn(Set.of(1L, 2L));

        sut.assignToChapter(new AssignSchoolCommand(SCHOOL_ID, CHAPTER_ID));

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberIds(Set.of(1L, 2L));
    }

    @Test
    @DisplayName("학교의 지부 배정을 해제하면 해당 학교 회원들의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_unassign_school_from_chapter() {
        given(loadSchoolPort.findSchoolDetailById(SCHOOL_ID)).willReturn(school());
        given(getMemberUseCase.listIdsBySchoolId(SCHOOL_ID)).willReturn(Set.of(1L, 2L));

        sut.unassignFromChapter(new UnassignSchoolCommand(SCHOOL_ID, GISU_ID));

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberIds(Set.of(1L, 2L));
    }

    @Test
    @DisplayName("학교의 지부를 변경하면 해당 학교 회원들의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_update_school_chapter() {
        given(loadSchoolPort.findById(SCHOOL_ID)).willReturn(school());
        given(loadChapterPort.findById(CHAPTER_ID)).willReturn(chapter());
        given(getMemberUseCase.listIdsBySchoolId(SCHOOL_ID)).willReturn(Set.of(1L, 2L));

        sut.updateSchool(SCHOOL_ID, new UpdateSchoolCommand("변경 학교", null, CHAPTER_ID, null, null, null));

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberIds(Set.of(1L, 2L));
    }

    @Test
    @DisplayName("학교를 삭제하면 삭제 대상 학교 회원들의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_delete_schools() {
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(SCHOOL_ID, OTHER_SCHOOL_ID)))
            .willReturn(Map.of(
                SCHOOL_ID, Set.of(1L, 2L),
                OTHER_SCHOOL_ID, Set.of(3L)
            ));

        sut.deleteSchools(List.of(SCHOOL_ID, OTHER_SCHOOL_ID));

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberIds(Set.of(1L, 2L, 3L));
    }

    private School school() {
        School school = School.create("테스트 학교", null, null);
        ReflectionTestUtils.setField(school, "id", SCHOOL_ID);
        return school;
    }

    private Chapter chapter() {
        Gisu gisu = Gisu.create(9L, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"), true);
        ReflectionTestUtils.setField(gisu, "id", GISU_ID);
        Chapter chapter = Chapter.create(gisu, "테스트 지부");
        ReflectionTestUtils.setField(chapter, "id", CHAPTER_ID);
        return chapter;
    }
}
