package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.global.exception.NotImplementedException;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({CurriculumQueryRepository.class, WeeklyCurriculumQueryRepository.class})
@DisplayName("Curriculum QueryDSL repository")
class CurriculumQueryRepositoryTest {

    @Autowired TestEntityManager entityManager;
    @Autowired CurriculumQueryRepository curriculumQueryRepository;
    @Autowired WeeklyCurriculumQueryRepository weeklyCurriculumQueryRepository;

    @Test
    @DisplayName("기수·파트 projection은 존재와 부재를 Optional로 구분한다")
    void finds_curriculum_projection_or_empty() {
        entityManager.persistAndFlush(Curriculum.create(991L, ChallengerPart.SPRINGBOOT, "커리큘럼"));
        entityManager.clear();

        var found = curriculumQueryRepository.findByGisuIdAndPart(991L, ChallengerPart.SPRINGBOOT);

        assertThat(found).isPresent().get().satisfies(value -> {
            assertThat(value.part()).isEqualTo(ChallengerPart.SPRINGBOOT);
            assertThat(value.title()).isEqualTo("커리큘럼");
        });
        assertThat(curriculumQueryRepository.findByGisuIdAndPart(992L, ChallengerPart.SPRINGBOOT))
            .isEmpty();
    }

    @Test
    @DisplayName("원본 워크북 존재 조회는 상태 미지정·지정과 결과 true·false를 모두 구분한다")
    void checks_workbook_existence_with_optional_status() {
        Curriculum curriculum = entityManager.persist(
            Curriculum.create(993L, ChallengerPart.SPRINGBOOT, "커리큘럼")
        );
        WeeklyCurriculum draftWeek = entityManager.persist(WeeklyCurriculum.create(
            curriculum, 1L, false, "1주차",
            Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-01-07T00:00:00Z")
        ));
        WeeklyCurriculum emptyWeek = entityManager.persist(WeeklyCurriculum.create(
            curriculum, 2L, false, "2주차",
            Instant.parse("2030-01-08T00:00:00Z"), Instant.parse("2030-01-14T00:00:00Z")
        ));
        entityManager.persist(OriginalWorkbook.createAsDraft(
            draftWeek, "초안", null, null, null, OriginalWorkbookType.MAIN
        ));
        OriginalWorkbook released = OriginalWorkbook.createAsReady(
            draftWeek, "배포", null, null, null, OriginalWorkbookType.EXTRA
        );
        released.changeStatus(OriginalWorkbookStatus.RELEASED, 1L);
        entityManager.persist(released);
        entityManager.flush();
        entityManager.clear();

        assertThat(weeklyCurriculumQueryRepository.existsOriginalWorkbook(draftWeek.getId(), null)).isTrue();
        assertThat(weeklyCurriculumQueryRepository.existsOriginalWorkbook(
            draftWeek.getId(), OriginalWorkbookStatus.RELEASED)).isTrue();
        assertThat(weeklyCurriculumQueryRepository.existsOriginalWorkbook(
            draftWeek.getId(), OriginalWorkbookStatus.READY)).isFalse();
        assertThat(weeklyCurriculumQueryRepository.existsOriginalWorkbook(emptyWeek.getId(), null)).isFalse();
    }

    @Test
    @DisplayName("자동 배포 조회 미구현 경로는 명시적인 예외를 반환한다")
    void unreleased_query_is_explicitly_not_implemented() {
        assertThatThrownBy(() -> curriculumQueryRepository.findUnreleasedWorkbookIdsWithStartDateBefore(
            Instant.parse("2030-01-01T00:00:00Z")))
            .isInstanceOf(NotImplementedException.class);
    }
}
