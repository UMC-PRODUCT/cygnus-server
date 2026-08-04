package com.umc.product.recruiting.application.service.query;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingApplicationScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingFormScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPartStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingQueryService implements
    GetRecruitingApplicationQueryUseCase,
    GetRecruitingFormQueryUseCase,
    ValidateRecruitingApplicationScopeUseCase,
    ValidateRecruitingFormScopeUseCase {

    /** 지원 현황 집계 대상 상태. 작성 중(DRAFT)·지원 취소(CANCELLED)는 제외한다. */
    private static final Set<RecruitingApplicationStatus> SUMMARY_STATUSES = EnumSet.complementOf(EnumSet.of(
        RecruitingApplicationStatus.DRAFT,
        RecruitingApplicationStatus.CANCELLED
    ));

    /** 파트별 집계 고정 슬롯. 모집 불가 트랙(INFRA_PLUS)을 제외한 파트를 sortOrder 순으로 항상 노출한다. */
    private static final List<ChallengerTrack> SUMMARY_PART_TRACKS = Arrays.stream(ChallengerTrack.values())
        .filter(track -> track != ChallengerTrack.INFRA_PLUS)
        .sorted(Comparator.comparingInt(ChallengerTrack::getSortOrder))
        .toList();

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetFormUseCase getFormUseCase;
    private final GetRecruitingApplicationQuestionScopeUseCase getQuestionScopeUseCase;

    @Override
    public RecruitingApplicationInfo getById(Long applicationId, Long requesterMemberId) {
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        application.validateApplicant(requesterMemberId);
        return RecruitingApplicationInfo.from(application);
    }

    @Override
    public FormWithStructureInfo getPublicFormStructure(
        Long applicationFormId,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    ) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getById(applicationFormId);
        if (applicationForm.getStatus() != RecruitingApplicationFormStatus.PUBLISHED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_PUBLISHED);
        }
        RecruitingApplicantProfile.validateChoices(applicationForm.getRound(), firstChoice, secondChoice);
        var scope = getQuestionScopeUseCase.getQuestionScope(applicationFormId, firstChoice, secondChoice);
        FormWithStructureInfo structure = getFormUseCase.getFormWithStructureByQuestionIds(
            applicationForm.getFormId(),
            scope.allowedQuestionIds()
        );
        List<FormWithStructureInfo.SectionWithQuestions> visibleSections = structure.sections().stream()
            .filter(section -> !section.questions().isEmpty())
            .toList();
        Set<Long> visibleSectionIds = visibleSections.stream()
            .map(FormWithStructureInfo.SectionWithQuestions::sectionId)
            .collect(java.util.stream.Collectors.toSet());
        return FormWithStructureInfo.builder()
            .formId(structure.formId())
            .createdMemberId(structure.createdMemberId())
            .title(structure.title())
            .description(structure.description())
            .status(structure.status())
            .isAnonymous(structure.isAnonymous())
            .allowDuplicateResponses(structure.allowDuplicateResponses())
            .createdAt(structure.createdAt())
            .updatedAt(structure.updatedAt())
            .sections(visibleSections.stream()
                .map(section -> filterConditionalDestinations(section, visibleSectionIds))
                .toList())
            .build();
    }

    private FormWithStructureInfo.SectionWithQuestions filterConditionalDestinations(
        FormWithStructureInfo.SectionWithQuestions section,
        Set<Long> visibleSectionIds
    ) {
        return FormWithStructureInfo.SectionWithQuestions.builder()
            .sectionId(section.sectionId())
            .title(section.title())
            .description(section.description())
            .orderNo(section.orderNo())
            .questions(section.questions().stream()
                .map(question -> FormWithStructureInfo.QuestionWithOptions.builder()
                    .questionId(question.questionId())
                    .title(question.title())
                    .description(question.description())
                    .type(question.type())
                    .isRequired(question.isRequired())
                    .orderNo(question.orderNo())
                    .options(question.options().stream()
                        .filter(option -> option.nextSectionId() == null
                            || visibleSectionIds.contains(option.nextSectionId()))
                        .toList())
                    .build())
                .toList())
            .build();
    }

    @Override
    public RecruitingStatusSummaryInfo getStatusSummary(RecruitingStatusSummaryQuery query) {
        Set<Long> accessibleSchoolIds = resolveSummarySchoolScope(query);
        List<SchoolDetailInfo> schools = listSummarySchools(query, accessibleSchoolIds);
        Set<Long> schoolIds = schools.stream().map(SchoolDetailInfo::schoolId).collect(java.util.stream.Collectors.toSet());
        List<RecruitingRound> rounds = listSummaryRounds(query, schoolIds);
        Set<Long> roundIds = rounds.stream().map(RecruitingRound::getId).collect(java.util.stream.Collectors.toSet());

        List<RecruitingApplicationSummaryRow> rows = schoolIds.isEmpty()
            || rounds.isEmpty()
            ? List.of()
            : loadApplicationPort.searchSummaryRows(
                query.gisuId(),
                schoolIds,
                query.roundIds().isEmpty() ? null : roundIds,
                SUMMARY_STATUSES
            );

        Map<Long, List<RecruitingApplicationSummaryRow>> rowsBySchool = rows.stream()
            .collect(java.util.stream.Collectors.groupingBy(RecruitingApplicationSummaryRow::schoolId));
        Map<Long, List<RecruitingRound>> roundsBySchool = rounds.stream()
            .collect(java.util.stream.Collectors.groupingBy(round -> round.getSeason().getSchoolId()));
        List<RecruitingSchoolStatusSummaryInfo> schoolSummaries = schools.stream()
            .map(school -> toSchoolSummary(
                school,
                rowsBySchool.getOrDefault(school.schoolId(), List.of()),
                roundsBySchool.getOrDefault(school.schoolId(), List.of())
            ))
            .toList();
        return new RecruitingStatusSummaryInfo(
            (long) rows.size(),
            countByStatus(rows),
            partSummaries(rows),
            schoolSummaries
        );
    }

    private List<SchoolDetailInfo> listSummarySchools(
        RecruitingStatusSummaryQuery query,
        Set<Long> accessibleSchoolIds
    ) {
        String schoolName = query.schoolName() == null ? null : query.schoolName().toLowerCase(Locale.ROOT);
        return getSchoolUseCase.getSchoolListByGisuId(query.gisuId()).stream()
            .filter(school -> accessibleSchoolIds == null
                || accessibleSchoolIds.contains(school.schoolId()))
            .filter(school -> query.schoolIds().isEmpty() || query.schoolIds().contains(school.schoolId()))
            .filter(school -> schoolName == null || school.schoolName().toLowerCase(Locale.ROOT).contains(schoolName))
            .sorted(Comparator.comparing(SchoolDetailInfo::schoolId))
            .toList();
    }

    /**
     * 중앙 운영진은 기수 전체, 지부장과 교내 운영진은 자신이 관리하는 학교만 조회합니다.
     * 반환값이 null이면 전체 학교 범위를 의미합니다.
     */
    private Set<Long> resolveSummarySchoolScope(RecruitingStatusSummaryQuery query) {
        Long memberId = query.requesterMemberId();
        Long gisuId = query.gisuId();
        if (getChallengerRoleUseCase.isCentralCoreInGisu(memberId, gisuId)
            || getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return null;
        }

        Set<Long> accessibleSchoolIds = getSchoolUseCase.getSchoolListByGisuId(gisuId).stream()
            .filter(school -> getChallengerRoleUseCase.isChapterPresidentInGisu(
                memberId,
                gisuId,
                school.chapterId()
            ) || getChallengerRoleUseCase.isSchoolAdminInGisu(memberId, gisuId, school.schoolId()))
            .map(SchoolDetailInfo::schoolId)
            .collect(java.util.stream.Collectors.toSet());

        if (accessibleSchoolIds.isEmpty()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SUMMARY_ACCESS_DENIED);
        }
        return accessibleSchoolIds;
    }

    private List<RecruitingRound> listSummaryRounds(RecruitingStatusSummaryQuery query, Set<Long> schoolIds) {
        List<Long> seasonIds = loadSeasonPort.listByGisuId(query.gisuId()).stream()
            .filter(season -> schoolIds.contains(season.getSchoolId()))
            .map(season -> season.getId())
            .toList();
        if (seasonIds.isEmpty()) {
            return List.of();
        }
        return loadRoundPort.listBySeasonIds(seasonIds).stream()
            .filter(round -> query.roundIds().isEmpty() || query.roundIds().contains(round.getId()))
            .sorted(Comparator.comparing((RecruitingRound round) -> round.getSeason().getSchoolId())
                .thenComparing(RecruitingRound::getRoundNo)
                .thenComparing(RecruitingRound::getId))
            .toList();
    }

    private RecruitingSchoolStatusSummaryInfo toSchoolSummary(
        SchoolDetailInfo school,
        List<RecruitingApplicationSummaryRow> rows,
        List<RecruitingRound> rounds
    ) {
        Map<Long, List<RecruitingApplicationSummaryRow>> rowsByRound = rows.stream()
            .collect(java.util.stream.Collectors.groupingBy(RecruitingApplicationSummaryRow::roundId));
        List<RecruitingRoundStatusSummaryInfo> roundSummaries = rounds.stream()
            .map(round -> {
                List<RecruitingApplicationSummaryRow> roundRows = rowsByRound.getOrDefault(round.getId(), List.of());
                return new RecruitingRoundStatusSummaryInfo(
                    round.getId(),
                    round.getTitle(),
                    round.getType(),
                    round.getRoundNo(),
                    (long) roundRows.size(),
                    countByStatus(roundRows),
                    partSummaries(roundRows)
                );
            })
            .toList();
        return new RecruitingSchoolStatusSummaryInfo(
            school.schoolId(),
            school.schoolName(),
            school.chapterId(),
            school.chapterName(),
            (long) rows.size(),
            countByStatus(rows),
            partSummaries(rows),
            roundSummaries
        );
    }

    private Map<RecruitingApplicationStatus, Long> countByStatus(List<RecruitingApplicationSummaryRow> rows) {
        Map<RecruitingApplicationStatus, Long> result = new EnumMap<>(RecruitingApplicationStatus.class);
        rows.forEach(row -> result.merge(row.applicationStatus(), 1L, Long::sum));
        return result;
    }

    /**
     * 1지망(firstChoice) 파트 기준으로 상태별 개수를 교차집계한다.
     * 지원자가 없는 파트도 0건으로 항상 포함하며(SUMMARY_PART_TRACKS 고정 슬롯), 파트별 합계는 totalCount와 일치한다.
     */
    private List<RecruitingPartStatusSummaryInfo> partSummaries(List<RecruitingApplicationSummaryRow> rows) {
        Map<ChallengerTrack, List<RecruitingApplicationSummaryRow>> rowsByPart = rows.stream()
            .filter(row -> row.firstChoice() != null)
            .collect(java.util.stream.Collectors.groupingBy(RecruitingApplicationSummaryRow::firstChoice));
        return SUMMARY_PART_TRACKS.stream()
            .map(track -> {
                List<RecruitingApplicationSummaryRow> partRows = rowsByPart.getOrDefault(track, List.of());
                return new RecruitingPartStatusSummaryInfo(track, (long) partRows.size(), countByStatus(partRows));
            })
            .toList();
    }

    @Override
    public boolean isRoundBelongsToSeason(Long roundId, Long seasonId) {
        if (roundId == null || seasonId == null) {
            return false;
        }
        return Objects.equals(loadRoundPort.getById(roundId).getSeason().getId(), seasonId);
    }

    @Override
    public boolean isApplicationBelongsToSeason(Long applicationId, Long seasonId) {
        if (applicationId == null || seasonId == null) {
            return false;
        }
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        return Objects.equals(application.getRound().getSeason().getId(), seasonId);
    }

    @Override
    public void validateRoundScope(Long applicationId, Long roundId) {
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        if (!Objects.equals(application.getRound().getId(), roundId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
    }

    @Override
    public boolean isApplicationFormBelongsToSeason(Long applicationFormId, Long seasonId) {
        if (applicationFormId == null || seasonId == null) {
            return false;
        }
        return Objects.equals(
            loadApplicationFormPort.getById(applicationFormId).getRound().getSeason().getId(),
            seasonId
        );
    }

    @Override
    public void validateSeasonScope(Long applicationFormId, Long seasonId) {
        if (!isApplicationFormBelongsToSeason(applicationFormId, seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND);
        }
    }

    @Override
    public boolean isFormBelongsToSeason(Long formId, Long seasonId) {
        if (formId == null || seasonId == null) {
            return false;
        }
        return loadApplicationFormPort.existsByFormIdAndSeasonId(formId, seasonId);
    }

}
