package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSearchQuery;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.enums.RecruitingRoundPhase;

@ExtendWith(MockitoExtension.class)
class RecruitingSeasonQueryServiceTest {

    @Mock
    LoadRecruitingSeasonPort loadSeasonPort;

    @Mock
    LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;

    @Mock
    LoadRecruitingRoundPort loadRoundPort;

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    @Mock
    Clock clock;

    @InjectMocks
    RecruitingSeasonQueryService sut;

    @Test
    @DisplayName("시즌의 쿼터와 차수 설정을 조회한다")
    void getSeasonConfiguration() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 100L);
        RecruitingSeasonTrackQuota quota = RecruitingSeasonTrackQuota.create(
            season,
            ChallengerTrack.PLAN,
            3
        );
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN),
            true,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            "안내",
            "contact"
        ));
        ReflectionTestUtils.setField(round, "id", 200L);
        given(loadSeasonPort.getById(100L)).willReturn(season);
        given(loadQuotaPort.listBySeasonId(100L)).willReturn(List.of(quota));
        given(loadRoundPort.listBySeasonId(100L)).willReturn(List.of(round));

        RecruitingSeasonConfigurationInfo info = sut.getBySeasonId(100L);

        assertThat(info.quotas()).singleElement().satisfies(found -> {
            assertThat(found.track()).isEqualTo(ChallengerTrack.PLAN);
            assertThat(found.targetCount()).isEqualTo(3);
        });
        assertThat(info.rounds()).singleElement().satisfies(found -> {
            assertThat(found.recruitableTracks()).containsExactly(ChallengerTrack.PLAN);
            assertThat(found.secondChoiceEnabled()).isTrue();
            assertThat(found.availabilityFormId()).isNull();
        });
    }

    @Test
    @DisplayName("시즌 목록은 현재 학교의 지부 소속을 기준으로 필터링하고 차수를 함께 반환한다")
    void searchSeasonsByCurrentChapter() {
        RecruitingSeason includedSeason = season(100L, 1L, 10L);
        RecruitingSeason excludedSeason = season(101L, 1L, 20L);
        RecruitingRound round = regularRound(includedSeason, 200L);
        SubjectAttributes subject = SubjectAttributes.builder().memberId(99L).build();
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(
            school(7L, "A 지부", 10L, "A 학교"),
            school(8L, "B 지부", 20L, "B 학교")
        ));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of(includedSeason, excludedSeason));
        given(checkPermissionUseCase.loadSubject(99L)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, readPermission(100L))).willReturn(true);
        given(loadRoundPort.listBySeasonIds(List.of(100L))).willReturn(List.of(round));

        var result = sut.searchSeasons(RecruitingSeasonSearchQuery.builder()
            .gisuId(1L)
            .chapterId(7L)
            .requesterMemberId(99L)
            .build());

        assertThat(result).singleElement().satisfies(found -> {
            assertThat(found.seasonId()).isEqualTo(100L);
            assertThat(found.chapterId()).isEqualTo(7L);
            assertThat(found.schoolName()).isEqualTo("A 학교");
            assertThat(found.rounds()).singleElement()
                .satisfies(foundRound -> assertThat(foundRound.id()).isEqualTo(200L));
        });
    }

    @Test
    @DisplayName("차수 목록은 시즌 필터를 적용한다")
    void searchRoundsBySeason() {
        RecruitingSeason allowedSeason = season(100L, 1L, 10L);
        RecruitingSeason deniedSeason = season(101L, 1L, 20L);
        RecruitingRound round = regularRound(allowedSeason, 200L);
        SubjectAttributes subject = SubjectAttributes.builder().memberId(99L).build();
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(
            school(7L, "A 지부", 10L, "A 학교"),
            school(7L, "A 지부", 20L, "B 학교")
        ));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of(allowedSeason, deniedSeason));
        given(checkPermissionUseCase.loadSubject(99L)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, readPermission(100L))).willReturn(true);
        given(loadRoundPort.listBySeasonIds(List.of(100L))).willReturn(List.of(round));

        var result = sut.searchRounds(RecruitingRoundSearchQuery.builder()
            .gisuId(1L)
            .seasonId(100L)
            .requesterMemberId(99L)
            .build());

        assertThat(result).singleElement().satisfies(found -> {
            assertThat(found.seasonId()).isEqualTo(100L);
            assertThat(found.round().id()).isEqualTo(200L);
        });
    }

    @Test
    @DisplayName("차수 목록에서 조회 권한이 없는 시즌은 제외한다")
    void searchRoundsExcludesUnauthorizedSeason() {
        RecruitingSeason allowedSeason = season(100L, 1L, 10L);
        RecruitingSeason deniedSeason = season(101L, 1L, 20L);
        RecruitingRound allowedRound = regularRound(allowedSeason, 200L);
        SubjectAttributes subject = SubjectAttributes.builder().memberId(99L).build();
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(
            school(7L, "A 지부", 10L, "A 학교"),
            school(7L, "A 지부", 20L, "B 학교")
        ));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of(allowedSeason, deniedSeason));
        given(checkPermissionUseCase.loadSubject(99L)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, readPermission(100L))).willReturn(true);
        given(checkPermissionUseCase.check(subject, readPermission(101L))).willReturn(false);
        given(loadRoundPort.listBySeasonIds(List.of(100L))).willReturn(List.of(allowedRound));

        var result = sut.searchRounds(RecruitingRoundSearchQuery.builder()
            .gisuId(1L)
            .chapterId(7L)
            .requesterMemberId(99L)
            .build());

        assertThat(result)
            .extracting(found -> found.round().id())
            .containsExactly(200L);
    }

    @Test
    @DisplayName("공개 모집은 접수 종료 시각부터 OPEN에서 제외하고 PAST에 포함한다")
    void publicRoundEndBoundaryIsPast() {
        Instant endAt = Instant.parse("2026-08-08T00:00:00Z");
        RecruitingSeason season = season(100L, 1L, 10L);
        RecruitingRound round = regularRound(season, 200L);
        round.open();
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L);
        ReflectionTestUtils.setField(form, "id", 300L);
        form.publish(round.getRecruitableTracks());
        given(clock.instant()).willReturn(endAt);
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(
            school(7L, "A 지부", 10L, "A 학교")
        ));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of(season));
        given(loadRoundPort.listBySeasonIds(List.of(100L))).willReturn(List.of(round));
        given(loadApplicationFormPort.listByRoundIds(List.of(200L))).willReturn(List.of(form));

        var openResult = sut.searchPublicRounds(RecruitingPublicRoundSearchQuery.builder()
            .gisuId(1L)
            .phase(RecruitingRoundPhase.OPEN)
            .build());
        var pastResult = sut.searchPublicRounds(RecruitingPublicRoundSearchQuery.builder()
            .gisuId(1L)
            .phase(RecruitingRoundPhase.PAST)
            .build());

        assertThat(openResult).isEmpty();
        assertThat(pastResult).singleElement().satisfies(group ->
            assertThat(group.rounds()).singleElement()
                .satisfies(found -> assertThat(found.roundId()).isEqualTo(200L))
        );
    }

    @Test
    @DisplayName("공개 모집은 복수 학교와 Round 내부는 OR, 학교명 조건과는 AND로 필터링한다")
    void searchPublicRoundsWithMultipleSchoolsRoundsAndSchoolName() {
        Instant now = Instant.parse("2026-08-04T00:00:00Z");
        RecruitingSeason alphaSeason = season(100L, 1L, 10L);
        RecruitingSeason betaSeason = season(101L, 1L, 20L);
        RecruitingRound includedRound = regularRound(alphaSeason, 200L);
        RecruitingRound excludedByRoundId = regularRound(alphaSeason, 201L);
        RecruitingRound excludedBySchoolName = regularRound(betaSeason, 202L);
        List<RecruitingRound> rounds = List.of(includedRound, excludedByRoundId, excludedBySchoolName);
        rounds.forEach(RecruitingRound::open);
        List<RecruitingApplicationForm> forms = rounds.stream()
            .map(this::publishedApplicationForm)
            .toList();
        given(clock.instant()).willReturn(now);
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(
            school(7L, "A 지부", 10L, "Alpha 대학교"),
            school(8L, "B 지부", 20L, "Beta 대학교")
        ));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of(alphaSeason, betaSeason));
        given(loadRoundPort.listBySeasonIds(List.of(100L))).willReturn(List.of(includedRound, excludedByRoundId));
        given(loadApplicationFormPort.listByRoundIds(List.of(200L)))
            .willReturn(forms.stream().filter(form -> form.getRound().getId().equals(200L)).toList());

        var result = sut.searchPublicRounds(RecruitingPublicRoundSearchQuery.builder()
            .gisuId(1L)
            .schoolIds(Set.of(10L, 20L))
            .roundIds(Set.of(200L, 202L))
            .schoolName("  ALPHA  ")
            .phase(RecruitingRoundPhase.OPEN)
            .build());

        assertThat(result).singleElement().satisfies(group -> {
            assertThat(group.schoolId()).isEqualTo(10L);
            assertThat(group.rounds()).singleElement()
                .satisfies(round -> assertThat(round.roundId()).isEqualTo(200L));
        });
    }

    private RecruitingSeason season(Long id, Long gisuId, Long schoolId) {
        RecruitingSeason season = RecruitingSeason.create(gisuId, schoolId);
        ReflectionTestUtils.setField(season, "id", id);
        return season;
    }

    private RecruitingRound regularRound(RecruitingSeason season, Long id) {
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN),
            false,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            "안내",
            "contact"
        ));
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private RecruitingApplicationForm publishedApplicationForm(RecruitingRound round) {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L + round.getId());
        ReflectionTestUtils.setField(form, "id", 300L + round.getId());
        form.publish(round.getRecruitableTracks());
        return form;
    }

    private SchoolDetailInfo school(Long chapterId, String chapterName, Long schoolId, String schoolName) {
        return new SchoolDetailInfo(
            chapterId,
            chapterName,
            schoolName,
            schoolId,
            null,
            null,
            List.of(),
            true,
            Instant.EPOCH,
            Instant.EPOCH
        );
    }

    private ResourcePermission readPermission(Long seasonId) {
        return ResourcePermission.of(
            ResourceType.RECRUITMENT,
            seasonId,
            com.umc.product.authorization.domain.PermissionType.READ
        );
    }
}
