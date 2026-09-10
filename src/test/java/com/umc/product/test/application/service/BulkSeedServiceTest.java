package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.test.application.port.in.command.dto.SeedBulkDataCommand;
import com.umc.product.test.application.port.in.command.dto.SeedBulkDataResult;
import com.umc.product.test.application.port.out.BulkSeedPort;
import com.umc.product.test.application.port.out.dto.BulkSeedBaseIds;
import com.umc.product.test.application.port.out.dto.SeedChallengerRow;
import com.umc.product.test.application.port.out.dto.SeedMemberRow;
import com.umc.product.test.application.port.out.dto.SeedScheduleParticipantRow;

@ExtendWith(MockitoExtension.class)
class BulkSeedServiceTest {

    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    BulkSeedPort bulkSeedPort;

    @Captor
    ArgumentCaptor<List<SeedMemberRow>> memberCaptor;
    @Captor
    ArgumentCaptor<List<SeedScheduleParticipantRow>> participantCaptor;
    @Captor
    ArgumentCaptor<List<SeedChallengerRow>> challengerCaptor;

    @InjectMocks
    BulkSeedService sut;

    private static final Long GISU_ID = 9L;

    @BeforeEach
    void setUp() {
        given(getGisuUseCase.getActiveGisu())
            .willReturn(new GisuInfo(GISU_ID, 9L, null, null, true, GisuLearningType.PART));
        // 학교 10개 (id 1..10) — 상위 5개(1..5)가 스큐 대상
        List<ChapterWithSchoolsInfo.SchoolInfo> schools = LongStream.rangeClosed(1, 10)
            .mapToObj(id -> new ChapterWithSchoolsInfo.SchoolInfo(id, "학교" + id))
            .toList();
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(GISU_ID))
            .willReturn(List.of(new ChapterWithSchoolsInfo(1L, "지부", schools)));
        given(bulkSeedPort.currentMaxIds()).willReturn(new BulkSeedBaseIds(100L, 200L, 300L, 400L));
    }

    private static SeedBulkDataCommand command(int memberCount) {
        return new SeedBulkDataCommand(memberCount, 2, 2, 3, 42L, 10);
    }

    @Test
    @DisplayName("Track 기수에는 기본 Track을 순환 배정하고 Part를 저장하지 않는다")
    void track_기수_기본_track_배정() {
        // Given
        given(getGisuUseCase.getActiveGisu())
            .willReturn(new GisuInfo(GISU_ID, 11L, null, null, true, GisuLearningType.TRACK));

        // When
        sut.seed(command(8));

        // Then
        verify(bulkSeedPort).insertChallengers(challengerCaptor.capture());
        assertThat(challengerCaptor.getValue()).allSatisfy(row -> {
            assertThat(row.part()).isNull();
            assertThat(row.tracks()).hasSize(1);
            assertThat(row.gisuId()).isEqualTo(GISU_ID);
        });
        assertThat(challengerCaptor.getValue()).flatExtracting(SeedChallengerRow::tracks)
            .containsExactly(
                ChallengerTrack.PLAN, ChallengerTrack.DESIGN,
                ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER,
                ChallengerTrack.PLAN, ChallengerTrack.DESIGN,
                ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("Part 기수에는 기존 일곱 Part를 순환 배정하고 Track을 비워 둔다")
    void part_기수_기존_part_배정() {
        // Given // When
        sut.seed(command(8));

        // Then
        verify(bulkSeedPort).insertChallengers(challengerCaptor.capture());
        assertThat(challengerCaptor.getValue()).allSatisfy(row -> assertThat(row.tracks()).isEmpty());
        assertThat(challengerCaptor.getValue()).extracting(SeedChallengerRow::part)
            .containsExactly(
                ChallengerPart.WEB, ChallengerPart.ANDROID, ChallengerPart.IOS, ChallengerPart.NODEJS,
                ChallengerPart.SPRINGBOOT, ChallengerPart.DESIGN, ChallengerPart.PLAN, ChallengerPart.WEB);
    }

    @Test
    @DisplayName("같은 seed 로 두 번 실행하면 동일한 멤버 데이터가 생성된다")
    void 같은_seed_동일_데이터() {
        // Given // When
        sut.seed(command(100));
        sut.seed(command(100));

        // Then
        verify(bulkSeedPort, org.mockito.Mockito.times(2)).insertMembers(memberCaptor.capture());
        List<List<SeedMemberRow>> runs = memberCaptor.getAllValues();
        assertThat(runs.get(0)).isEqualTo(runs.get(1));
    }

    @Test
    @DisplayName("상위 5개 학교에 멤버의 약 절반이 배정된다 (스큐)")
    void 상위_학교_스큐_배정() {
        // Given // When
        sut.seed(command(1000));

        // Then
        verify(bulkSeedPort).insertMembers(memberCaptor.capture());
        List<SeedMemberRow> members = memberCaptor.getValue();
        long topSchoolMembers = members.stream().filter(m -> m.schoolId() <= 5).count();
        assertThat(members).hasSize(1000);
        assertThat(topSchoolMembers).isBetween(450L, 550L);
    }

    @Test
    @DisplayName("id 는 현재 max 이후 구간에서 부여되고, 멤버마다 서로 다른 스케줄에 배정된다")
    void id_부여와_스케줄_배정() {
        // Given // When
        SeedBulkDataResult result = sut.seed(command(100));

        // Then
        verify(bulkSeedPort).insertMembers(memberCaptor.capture());
        verify(bulkSeedPort).insertScheduleParticipants(participantCaptor.capture());
        assertThat(memberCaptor.getValue().get(0).id()).isEqualTo(101L);
        assertThat(result.memberIds()).allSatisfy(id -> assertThat(id).isGreaterThan(100L));

        // 멤버당 2개 참여, 같은 멤버가 같은 스케줄에 중복 배정되지 않는다
        List<SeedScheduleParticipantRow> participants = participantCaptor.getValue();
        assertThat(participants).hasSize(200);
        assertThat(participants.stream().map(p -> p.memberId() + ":" + p.scheduleId()).distinct())
            .hasSize(200);
        // 스케줄 id 는 base(300) 이후 scheduleCount(최소 4) 범위 안
        assertThat(participants).allSatisfy(p ->
            assertThat(p.scheduleId()).isBetween(301L, 304L));

        verify(bulkSeedPort).finalizeBulkLoad();
    }
}
