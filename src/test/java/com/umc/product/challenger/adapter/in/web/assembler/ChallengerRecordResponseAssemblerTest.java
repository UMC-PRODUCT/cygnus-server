package com.umc.product.challenger.adapter.in.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordSummaryResponse;
import com.umc.product.challenger.adapter.in.web.dto.response.UnusedChallengerRecordStatisticsResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.query.GetUnusedChallengerRecordStatisticsUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerRecordInfo;
import com.umc.product.challenger.application.port.in.query.dto.ListChallengerRecordsQuery;
import com.umc.product.challenger.application.port.in.query.dto.UnusedChallengerRecordCountInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.global.response.PageResponse;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

/**
 * ChallengerRecordResponseAssembler 단위 테스트.
 * <p>
 * P2-c 핵심 검증: 같은 학교(schoolId)가 서로 다른 기수(gisuId)에서 다른 지부(chapter)에 속할 때,
 * 기수 차원을 유지하는 중첩 맵 {@code Map<gisuId, Map<schoolId, SchoolDetailInfo>>}을 통해
 * 각 (기수, 학교) 행이 해당 기수의 올바른 chapter 정보를 반환하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRecordResponseAssembler")
class ChallengerRecordResponseAssemblerTest {

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    GetChallengerRecordUseCase getChallengerRecordUseCase;

    @Mock
    GetUnusedChallengerRecordStatisticsUseCase getUnusedChallengerRecordStatisticsUseCase;

    @InjectMocks
    ChallengerRecordResponseAssembler assembler;

    // ─── 공통 픽스처 ────────────────────────────────────────────────────────────
    // 학교 ID 100이 기수 1에서는 "A 지부"(chapterId=10), 기수 2에서는 "B 지부"(chapterId=20)에 속한다.
    private static final long GISU_ID_1 = 1L;
    private static final long GISU_ID_2 = 2L;
    private static final long SCHOOL_ID = 100L;
    private static final long CHAPTER_ID_FOR_GISU1 = 10L;
    private static final long CHAPTER_ID_FOR_GISU2 = 20L;
    private static final String CHAPTER_NAME_FOR_GISU1 = "A 지부";
    private static final String CHAPTER_NAME_FOR_GISU2 = "B 지부";
    private static final String SCHOOL_NAME = "UMC 대학교";

    private SchoolDetailInfo schoolInfoFor(long chapterId, String chapterName) {
        return new SchoolDetailInfo(
            chapterId,
            chapterName,
            SCHOOL_NAME,
            SCHOOL_ID,
            null,
            null,
            List.of(),
            true,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z")
        );
    }

    private GisuInfo gisuInfo(long gisuId, long generation) {
        return new GisuInfo(gisuId, generation, Instant.EPOCH, Instant.MAX, true);
    }

    // ─── unusedStatistics() 테스트 ──────────────────────────────────────────────
    @Nested
    @DisplayName("unusedStatistics()")
    class UnusedStatistics {

        @Test
        @DisplayName("같은 학교가 서로 다른 기수에서 다른 지부에 속할 때, 각 행은 해당 기수의 schoolName을 반환한다")
        void 같은_학교_다른_기수_다른_chapter_schoolName_올바르게_매핑() {
            // Given
            List<UnusedChallengerRecordCountInfo> counts = List.of(
                new UnusedChallengerRecordCountInfo(GISU_ID_1, SCHOOL_ID, 5L),
                new UnusedChallengerRecordCountInfo(GISU_ID_2, SCHOOL_ID, 3L)
            );
            given(getUnusedChallengerRecordStatisticsUseCase.listUnusedCountByGisuAndSchool())
                .willReturn(counts);

            given(getGisuUseCase.getByIds(Set.of(GISU_ID_1, GISU_ID_2))).willReturn(List.of(
                gisuInfo(GISU_ID_1, 1L),
                gisuInfo(GISU_ID_2, 2L)
            ));

            // 기수 1 → schoolId 100 = "A 지부", 기수 2 → schoolId 100 = "B 지부"
            given(getSchoolUseCase.getSchoolListByGisuIds(Set.of(GISU_ID_1, GISU_ID_2))).willReturn(Map.of(
                GISU_ID_1, List.of(schoolInfoFor(CHAPTER_ID_FOR_GISU1, CHAPTER_NAME_FOR_GISU1)),
                GISU_ID_2, List.of(schoolInfoFor(CHAPTER_ID_FOR_GISU2, CHAPTER_NAME_FOR_GISU2))
            ));

            // When
            UnusedChallengerRecordStatisticsResponse response = assembler.unusedStatistics();

            // Then
            assertThat(response.totalUnusedCount()).isEqualTo(8L);
            assertThat(response.rows()).hasSize(2);

            UnusedChallengerRecordStatisticsResponse.Row row1 = response.rows().stream()
                .filter(r -> r.gisuId().equals(GISU_ID_1))
                .findFirst()
                .orElseThrow();
            UnusedChallengerRecordStatisticsResponse.Row row2 = response.rows().stream()
                .filter(r -> r.gisuId().equals(GISU_ID_2))
                .findFirst()
                .orElseThrow();

            // 기수 1 행: schoolName은 기수 1의 데이터에서 가져온다
            assertThat(row1.schoolId()).isEqualTo(SCHOOL_ID);
            assertThat(row1.schoolName()).isEqualTo(SCHOOL_NAME);
            assertThat(row1.unusedCount()).isEqualTo(5L);
            assertThat(row1.generation()).isEqualTo(1L);

            // 기수 2 행: schoolName은 기수 2의 데이터에서 가져온다
            assertThat(row2.schoolId()).isEqualTo(SCHOOL_ID);
            assertThat(row2.schoolName()).isEqualTo(SCHOOL_NAME);
            assertThat(row2.unusedCount()).isEqualTo(3L);
            assertThat(row2.generation()).isEqualTo(2L);
        }

        @Test
        @DisplayName("결과가 비어 있으면 빈 rows와 totalUnusedCount=0을 반환한다")
        void 결과가_비어_있으면_빈_응답을_반환한다() {
            // Given
            given(getUnusedChallengerRecordStatisticsUseCase.listUnusedCountByGisuAndSchool())
                .willReturn(List.of());

            // When
            UnusedChallengerRecordStatisticsResponse response = assembler.unusedStatistics();

            // Then
            assertThat(response.totalUnusedCount()).isZero();
            assertThat(response.rows()).isEmpty();
        }
    }

    // ─── search() 테스트 ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("같은 학교가 서로 다른 기수에서 다른 지부에 속할 때, 각 항목은 해당 기수의 chapterId와 chapterName을 반환한다")
        void 같은_학교_다른_기수_다른_chapter_올바르게_매핑() {
            // Given
            ChallengerRecordInfo recordGisu1 = ChallengerRecordInfo.builder()
                .id(1L)
                .code("CODE-1")
                .gisuId(GISU_ID_1)
                .schoolId(SCHOOL_ID)
                .chapterId(CHAPTER_ID_FOR_GISU1)
                .part(ChallengerPart.SPRINGBOOT)
                .challengerRoleType(ChallengerRoleType.SCHOOL_PART_LEADER)
                .isUsed(false)
                .build();

            ChallengerRecordInfo recordGisu2 = ChallengerRecordInfo.builder()
                .id(2L)
                .code("CODE-2")
                .gisuId(GISU_ID_2)
                .schoolId(SCHOOL_ID)
                .chapterId(CHAPTER_ID_FOR_GISU2)
                .part(ChallengerPart.SPRINGBOOT)
                .challengerRoleType(ChallengerRoleType.SCHOOL_PART_LEADER)
                .isUsed(false)
                .build();

            Page<ChallengerRecordInfo> page = new PageImpl<>(
                List.of(recordGisu1, recordGisu2),
                PageRequest.of(0, 10),
                2
            );

            ListChallengerRecordsQuery query = ListChallengerRecordsQuery.builder()
                .pageable(PageRequest.of(0, 10))
                .build();

            given(getChallengerRecordUseCase.search(query)).willReturn(page);
            given(getGisuUseCase.getByIds(Set.of(GISU_ID_1, GISU_ID_2))).willReturn(List.of(
                gisuInfo(GISU_ID_1, 1L),
                gisuInfo(GISU_ID_2, 2L)
            ));

            // 기수 1 → schoolId 100 = "A 지부"(chapterId=10), 기수 2 → schoolId 100 = "B 지부"(chapterId=20)
            given(getSchoolUseCase.getSchoolListByGisuIds(Set.of(GISU_ID_1, GISU_ID_2))).willReturn(Map.of(
                GISU_ID_1, List.of(schoolInfoFor(CHAPTER_ID_FOR_GISU1, CHAPTER_NAME_FOR_GISU1)),
                GISU_ID_2, List.of(schoolInfoFor(CHAPTER_ID_FOR_GISU2, CHAPTER_NAME_FOR_GISU2))
            ));

            // When
            PageResponse<ChallengerRecordSummaryResponse> result = assembler.search(query);

            // Then
            assertThat(result.content()).hasSize(2);

            ChallengerRecordSummaryResponse summaryForGisu1 = result.content().stream()
                .filter(r -> r.gisuId().equals(GISU_ID_1))
                .findFirst()
                .orElseThrow();

            ChallengerRecordSummaryResponse summaryForGisu2 = result.content().stream()
                .filter(r -> r.gisuId().equals(GISU_ID_2))
                .findFirst()
                .orElseThrow();

            // 기수 1 항목: 기수 1의 chapter 정보(A 지부, chapterId=10)를 반환해야 한다
            assertThat(summaryForGisu1.schoolId()).isEqualTo(SCHOOL_ID);
            assertThat(summaryForGisu1.schoolName()).isEqualTo(SCHOOL_NAME);
            assertThat(summaryForGisu1.chapterId()).isEqualTo(CHAPTER_ID_FOR_GISU1);
            assertThat(summaryForGisu1.chapterName()).isEqualTo(CHAPTER_NAME_FOR_GISU1);

            // 기수 2 항목: 기수 2의 chapter 정보(B 지부, chapterId=20)를 반환해야 한다
            // (버그 시: (a, b) -> a 병합으로 한쪽 기수의 chapter가 덮어씌워짐)
            assertThat(summaryForGisu2.schoolId()).isEqualTo(SCHOOL_ID);
            assertThat(summaryForGisu2.schoolName()).isEqualTo(SCHOOL_NAME);
            assertThat(summaryForGisu2.chapterId()).isEqualTo(CHAPTER_ID_FOR_GISU2);
            assertThat(summaryForGisu2.chapterName()).isEqualTo(CHAPTER_NAME_FOR_GISU2);
        }

        @Test
        @DisplayName("페이지가 비어 있으면 UseCase 학교/기수 조회 없이 빈 페이지를 반환한다")
        void 페이지가_비어_있으면_빈_페이지를_반환한다() {
            // Given
            Page<ChallengerRecordInfo> emptyPage = Page.empty(PageRequest.of(0, 10));
            ListChallengerRecordsQuery query = ListChallengerRecordsQuery.builder()
                .pageable(PageRequest.of(0, 10))
                .build();

            given(getChallengerRecordUseCase.search(query)).willReturn(emptyPage);

            // When
            PageResponse<ChallengerRecordSummaryResponse> result = assembler.search(query);

            // Then
            assertThat(result.content()).isEmpty();
        }
    }
}
