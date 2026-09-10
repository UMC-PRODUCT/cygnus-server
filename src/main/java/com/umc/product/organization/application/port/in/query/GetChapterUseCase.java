package com.umc.product.organization.application.port.in.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;

public interface GetChapterUseCase {

    List<ChapterInfo> getAllChapters();

    List<ChapterInfo> listByGisuId(Long gisuId);

    Map<Long, List<ChapterInfo>> listByGisuIds(Set<Long> gisuIds);

    /**
     * 기수와 학교 정보로 지부 정보를 조회합니다.
     */
    ChapterInfo byGisuAndSchool(Long gisuId, Long schoolId);

    /**
     * 해당 기수에 학교가 배정된 지부가 없으면 빈 값을 반환합니다.
     */
    Optional<ChapterInfo> findByGisuAndSchool(Long gisuId, Long schoolId);

    List<ChapterInfo> getChaptersBySchool(Long schoolId);

    /**
     * 여러 학교가 속한 지부 정보를 1번 쿼리로 일괄 조회합니다. (학교 ↔ 지부 N:M)
     */
    List<ChapterInfo> getChaptersBySchoolIds(Collection<Long> schoolIds);

    List<ChapterWithSchoolsInfo> getChaptersWithSchoolsByGisuId(Long gisuId);

    Map<Long, List<ChapterWithSchoolsInfo>> getChaptersWithSchoolsByGisuIds(Set<Long> gisuIds);

    ChapterInfo getChapterById(Long chapterId);

    /**
     * 여러 gisuId와 schoolId 조합에 해당하는 ChapterInfo를 1번 쿼리로 일괄 조회
     *
     * @return gisuId → (schoolId → ChapterInfo) 중첩 맵
     */
    Map<Long, Map<Long, ChapterInfo>> getChapterMapByGisuIdsAndSchoolIds(Set<Long> gisuIds, Set<Long> schoolIds);
}
