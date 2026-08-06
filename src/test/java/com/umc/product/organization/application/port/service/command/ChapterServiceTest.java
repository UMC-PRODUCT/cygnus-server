package com.umc.product.organization.application.port.service.command;

import static org.mockito.ArgumentMatchers.any;
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
import com.umc.product.organization.application.port.in.command.dto.CreateChapterCommand;
import com.umc.product.organization.application.port.out.command.SaveChapterPort;
import com.umc.product.organization.application.port.out.command.SaveChapterSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadChapterSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChapterService")
class ChapterServiceTest {

    private static final Long GISU_ID = 9L;
    private static final Long CHAPTER_ID = 40L;
    private static final Long SCHOOL_ID = 30L;
    private static final Long OTHER_SCHOOL_ID = 31L;

    @Mock
    LoadGisuPort loadGisuPort;

    @Mock
    LoadChapterPort loadChapterPort;

    @Mock
    LoadSchoolPort loadSchoolPort;

    @Mock
    LoadChapterSchoolPort loadChapterSchoolPort;

    @Mock
    SaveChapterPort saveChapterPort;

    @Mock
    SaveChapterSchoolPort saveChapterSchoolPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @InjectMocks
    ChapterService sut;

    @Test
    @DisplayName("학교와 함께 지부를 생성하면 해당 학교 회원들의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_create_chapter_with_schools() {
        Gisu gisu = gisu();
        given(loadGisuPort.getById(GISU_ID)).willReturn(gisu);
        given(loadChapterPort.findByGisuId(GISU_ID)).willReturn(List.of());
        given(saveChapterPort.save(any(Chapter.class))).willAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            ReflectionTestUtils.setField(chapter, "id", CHAPTER_ID);
            return chapter;
        });
        given(loadSchoolPort.findAllByIds(List.of(SCHOOL_ID, OTHER_SCHOOL_ID)))
            .willReturn(List.of(school(SCHOOL_ID), school(OTHER_SCHOOL_ID)));
        given(loadChapterSchoolPort.findByGisuId(GISU_ID)).willReturn(List.of());
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(SCHOOL_ID, OTHER_SCHOOL_ID)))
            .willReturn(Map.of(
                SCHOOL_ID, Set.of(1L, 2L),
                OTHER_SCHOOL_ID, Set.of(3L)
            ));

        sut.create(new CreateChapterCommand(GISU_ID, "테스트 지부", List.of(SCHOOL_ID, OTHER_SCHOOL_ID)));

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberIds(Set.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("지부를 삭제하면 연결된 학교 회원들의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_delete_chapter() {
        Chapter chapter = chapter(CHAPTER_ID);
        given(loadChapterPort.findById(CHAPTER_ID)).willReturn(chapter);
        given(loadChapterSchoolPort.findByGisuId(GISU_ID)).willReturn(List.of(
            ChapterSchool.create(chapter, school(SCHOOL_ID)),
            ChapterSchool.create(chapter(41L), school(OTHER_SCHOOL_ID))
        ));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(SCHOOL_ID))).willReturn(Map.of(SCHOOL_ID, Set.of(1L, 2L)));

        sut.delete(CHAPTER_ID);

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberIds(Set.of(1L, 2L));
    }

    private Gisu gisu() {
        Gisu gisu = Gisu.create(9L, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"), true);
        ReflectionTestUtils.setField(gisu, "id", GISU_ID);
        return gisu;
    }

    private Chapter chapter(Long id) {
        Chapter chapter = Chapter.create(gisu(), "테스트 지부");
        ReflectionTestUtils.setField(chapter, "id", id);
        return chapter;
    }

    private School school(Long id) {
        School school = School.create("테스트 학교 " + id, null, null);
        ReflectionTestUtils.setField(school, "id", id);
        return school;
    }
}
