package com.umc.product.integration.recruiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingAdminFormStructureInfo;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.support.IntegrationTestSupport;

/**
 * 편집기 왕복(Upsert 저장 -> 조회)이 구조를 그대로 복원하는지 확인한다.
 * <p>
 * COMMON/TRACK 정책은 Form 모듈이 아닌 recruiting 쪽에 저장되므로, 서비스 단위 stub으로는
 * 병합 누락을 잡을 수 없다. 실제 Form 모듈과 DB를 함께 태워야 section 정책, orderNo, 필수 여부,
 * 조건부 이동 대상이 저장된 그대로 돌아오는지 증명할 수 있다.
 */
class RecruitingAdminFormStructureQueryIntegrationTest extends IntegrationTestSupport {

    private static final Long REQUESTER_MEMBER_ID = 9101L;
    private static final ChallengerTrack RECRUITABLE_TRACK = ChallengerTrack.WEB_PRODUCT_ENGINEER;

    @Autowired
    UpsertRecruitingApplicationFormUseCase upsertApplicationFormUseCase;

    @Autowired
    UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;

    @Autowired
    GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;

    @Autowired
    SaveRecruitingSeasonPort saveSeasonPort;

    @Autowired
    SaveRecruitingRoundPort saveRoundPort;

    @Test
    @DisplayName("Upsert로 저장한 지원 Form 구조는 조회에서 section 정책까지 그대로 복원된다")
    void upsertThenGetRestoresWholeStructure() {
        // Given
        RecruitingSeason season = saveSeasonPort.save(RecruitingSeason.create(9L, 201L));
        RecruitingRound round = saveRoundPort.save(
            RecruitingRound.createRegular(season, "본모집", configuration())
        );
        upsertApplicationForm(season.getId(), round.getId());

        // When
        RecruitingAdminFormStructureInfo structure =
            getRecruitingFormQueryUseCase.getAdminFormStructure(season.getId(), round.getId());

        // Then
        assertThat(structure.exists()).isTrue();
        assertThat(structure.applicationFormId()).isNotNull();
        assertThat(structure.formId()).isNotNull();
        assertThat(structure.sections()).hasSize(2);

        RecruitingAdminFormStructureInfo.SectionInfo common = structure.sections().get(0);
        assertThat(common.type()).isEqualTo(RecruitingFormSectionType.COMMON);
        assertThat(common.track()).isNull();
        assertThat(common.orderNo()).isNotNull();
        assertThat(common.questions()).singleElement().satisfies(question -> {
            assertThat(question.title()).isEqualTo("지원 동기");
            assertThat(question.type()).isEqualTo(QuestionType.RADIO);
            assertThat(question.required()).isTrue();
            assertThat(question.orderNo()).isNotNull();
            assertThat(question.options()).hasSize(2);
            assertThat(question.options()).allSatisfy(option ->
                assertThat(option.orderNo()).isNotNull()
            );
        });

        RecruitingAdminFormStructureInfo.SectionInfo track = structure.sections().get(1);
        assertThat(track.type()).isEqualTo(RecruitingFormSectionType.TRACK);
        assertThat(track.track()).isEqualTo(RECRUITABLE_TRACK);
        assertThat(track.questions()).singleElement().satisfies(question -> {
            assertThat(question.title()).isEqualTo("사용 기술");
            assertThat(question.required()).isFalse();
            assertThat(question.orderNo()).isNotNull();
        });

        // section은 요청 순서대로 복원되어야 편집 화면이 저장 당시 배치를 재현할 수 있다.
        assertThat(common.orderNo()).isLessThan(track.orderNo());
        assertThat(common.questions().get(0).options())
            .extracting(RecruitingAdminFormStructureInfo.OptionInfo::orderNo)
            .isSorted();

        // 조건부 이동은 clientKey가 아니라 저장된 sectionId로 복원된다.
        assertThat(common.questions().get(0).options().get(0).nextSectionId())
            .isEqualTo(track.sectionId());
        assertThat(common.questions().get(0).options().get(1).nextSectionId()).isNull();
    }

    @Test
    @DisplayName("지원 Form을 만들지 않은 차수는 빈 구조를 반환한다")
    void roundWithoutFormReturnsEmptyStructure() {
        // Given
        RecruitingSeason season = saveSeasonPort.save(RecruitingSeason.create(9L, 202L));
        RecruitingRound round = saveRoundPort.save(
            RecruitingRound.createRegular(season, "본모집", configuration())
        );

        // When
        RecruitingAdminFormStructureInfo structure =
            getRecruitingFormQueryUseCase.getAdminFormStructure(season.getId(), round.getId());

        // Then
        assertThat(structure.exists()).isFalse();
        assertThat(structure.applicationFormId()).isNull();
        assertThat(structure.formId()).isNull();
        assertThat(structure.sections()).isEmpty();
    }

    @Test
    @DisplayName("OPEN 차수의 게시된 Form도 상태와 무관하게 전체 구조를 반환한다")
    void publishedFormIsStillReadableForEditing() {
        // Given
        RecruitingSeason season = saveSeasonPort.save(RecruitingSeason.create(9L, 205L));
        RecruitingRound round = saveRoundPort.save(
            RecruitingRound.createRegular(season, "본모집", configuration())
        );
        // 게시 검증이 COMMON -> TRACK 조건부 이동을 금지하므로 이동 없는 Form으로 구성한다.
        upsertApplicationForm(season.getId(), round.getId(), false);
        updateRoundStatusUseCase.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(season.getId())
            .roundId(round.getId())
            .status(RecruitingRoundStatus.OPEN)
            .requesterMemberId(REQUESTER_MEMBER_ID)
            .build());

        // When
        RecruitingAdminFormStructureInfo structure =
            getRecruitingFormQueryUseCase.getAdminFormStructure(season.getId(), round.getId());

        // Then
        // 공개 조회는 PUBLISHED만 허용하고 지망 트랙으로 section을 걸러내지만,
        // 편집기 조회는 상태 검증도 트랙 필터링도 하지 않는다.
        assertThat(structure.exists()).isTrue();
        assertThat(structure.status()).isEqualTo(RecruitingApplicationFormStatus.PUBLISHED);
        assertThat(structure.sections()).hasSize(2);
        assertThat(structure.sections())
            .extracting(RecruitingAdminFormStructureInfo.SectionInfo::type)
            .containsExactly(RecruitingFormSectionType.COMMON, RecruitingFormSectionType.TRACK);
    }

    @Test
    @DisplayName("다른 시즌의 차수 ID로는 지원 Form 구조를 조회할 수 없다")
    void roundOfAnotherSeasonIsRejected() {
        // Given
        RecruitingSeason season = saveSeasonPort.save(RecruitingSeason.create(9L, 203L));
        RecruitingSeason otherSeason = saveSeasonPort.save(RecruitingSeason.create(9L, 204L));
        RecruitingRound round = saveRoundPort.save(
            RecruitingRound.createRegular(season, "본모집", configuration())
        );
        upsertApplicationForm(season.getId(), round.getId());

        // When & Then
        assertThatThrownBy(() ->
            getRecruitingFormQueryUseCase.getAdminFormStructure(otherSeason.getId(), round.getId())
        ).isInstanceOf(RecruitingDomainException.class);
    }

    private RecruitingRoundConfiguration configuration() {
        return RecruitingRoundConfiguration.of(
            List.of(RECRUITABLE_TRACK),
            false,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            "안내",
            "연락처"
        );
    }

    private void upsertApplicationForm(Long seasonId, Long roundId) {
        upsertApplicationForm(seasonId, roundId, true);
    }

    private void upsertApplicationForm(Long seasonId, Long roundId, boolean withConditionalTransition) {
        upsertApplicationFormUseCase.upsert(UpsertRecruitingApplicationFormCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .requesterMemberId(REQUESTER_MEMBER_ID)
            .description("지원서")
            .sections(List.of(
                UpsertRecruitingApplicationFormCommand.SectionEntry.builder()
                    .clientKey("common")
                    .title("공통")
                    .type(RecruitingFormSectionType.COMMON)
                    .questions(List.of(UpsertRecruitingApplicationFormCommand.QuestionEntry.builder()
                        .type(QuestionType.RADIO)
                        .title("지원 동기")
                        .required(true)
                        .options(List.of(
                            UpsertRecruitingApplicationFormCommand.OptionEntry.builder()
                                .content("파트 문항으로")
                                .nextSectionKey(withConditionalTransition ? "track" : null)
                                .build(),
                            UpsertRecruitingApplicationFormCommand.OptionEntry.builder()
                                .content("이동 없음")
                                .build()
                        ))
                        .build()))
                    .build(),
                UpsertRecruitingApplicationFormCommand.SectionEntry.builder()
                    .clientKey("track")
                    .title("파트 문항")
                    .type(RecruitingFormSectionType.TRACK)
                    .track(RECRUITABLE_TRACK)
                    .questions(List.of(UpsertRecruitingApplicationFormCommand.QuestionEntry.builder()
                        .type(QuestionType.SHORT_TEXT)
                        .title("사용 기술")
                        .required(false)
                        .build()))
                    .build()
            ))
            .build());
    }
}
