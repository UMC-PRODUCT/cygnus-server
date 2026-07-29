package com.umc.product.curriculum.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.port.out.SearchWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.persistence.ConstraintViolationInspector;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WeeklyBestWorkbookPersistenceAdapter implements
    LoadWeeklyBestWorkbookPort, SaveWeeklyBestWorkbookPort, SearchWeeklyBestWorkbookPort {

    private static final String UNIQUE_GROUP_WEEK_CONSTRAINT =
        "uk_weekly_best_workbook_study_group_week";

    private final WeeklyBestWorkbookJpaRepository jpaRepository;
    private final WeeklyBestWorkbookQueryRepository queryRepository;

    @Override
    public WeeklyBestWorkbook getById(Long id) {
        return jpaRepository.findById(id)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_NOT_FOUND));
    }

    @Override
    public List<BestWorkbookHolder> findHolders(
        Collection<Long> studyGroupIds,
        Collection<Long> weeklyCurriculumIds
    ) {
        return queryRepository.findHolders(studyGroupIds, weeklyCurriculumIds);
    }

    @Override
    public boolean existsByWeeklyCurriculumIdAndStudyGroupId(Long weeklyCurriculumId, Long studyGroupId) {
        return jpaRepository.existsByWeeklyCurriculum_IdAndStudyGroupId(weeklyCurriculumId, studyGroupId);
    }

    @Override
    public boolean existsByMemberIdAndWeeklyCurriculumIdAndStudyGroupId(
        Long memberId,
        Long weeklyCurriculumId,
        Long studyGroupId
    ) {
        return jpaRepository.existsByMemberIdAndWeeklyCurriculum_IdAndStudyGroupId(
            memberId,
            weeklyCurriculumId,
            studyGroupId
        );
    }

    @Override
    public WeeklyBestWorkbook save(WeeklyBestWorkbook weeklyBestWorkbook) {
        try {
            WeeklyBestWorkbook saved = jpaRepository.save(weeklyBestWorkbook);
            jpaRepository.flush();
            return saved;
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolationInspector.matches(e, UNIQUE_GROUP_WEEK_CONSTRAINT)) {
                throw new CurriculumDomainException(CurriculumErrorCode.WEEKLY_BEST_ALREADY_EXISTS, e);
            }
            throw e;
        }
    }

    @Override
    public void delete(WeeklyBestWorkbook weeklyBestWorkbook) {
        jpaRepository.delete(weeklyBestWorkbook);
    }

    @Override
    public Page<WeeklyBestWorkbook> searchBestWorkbooks(GetBestWorkbooksQuery query) {
        return queryRepository.searchBestWorkbooks(query);
    }
}
