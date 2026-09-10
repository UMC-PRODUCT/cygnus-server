package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.member.application.port.in.command.RegisterEmailMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersCommand;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersResult;

@ExtendWith(MockitoExtension.class)
class ChallengerSeedServiceTest {

    @Mock
    DummyMemberFactory dummyMemberFactory;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    RegisterEmailMemberUseCase registerEmailMemberUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    ManageChallengerUseCase manageChallengerUseCase;

    @InjectMocks
    ChallengerSeedService sut;
    List<TermConsents> consents;

    @BeforeEach
    void setUp() {
        consents = List.of();
        lenient().when(getGisuUseCase.getById(anyLong())).thenAnswer(inv ->
            new GisuInfo(inv.getArgument(0), 10L, null, null, true, GisuLearningType.PART));
        lenient().when(getMemberUseCase.countAll()).thenReturn(0L);
        lenient().when(dummyMemberFactory.snapshotMandatoryConsents()).thenReturn(consents);
        lenient().when(dummyMemberFactory.nextEmailCommandWithSchool(anyLong(), anyLong(), any()))
            .thenReturn(mock(EmailRegisterMemberCommand.class));
        AtomicLong memberIdCounter = new AtomicLong(1L);
        lenient().when(registerEmailMemberUseCase.batchRegister(any()))
            .thenAnswer(inv -> nextIds(memberIdCounter, inv.getArgument(0, List.class).size()));
    }

    @Test
    @DisplayName("parts 가 null 이면 ADMIN 제외 모든 파트가 대상이 된다")
    void parts_기본값_ADMIN_제외() {
        // Given
        Long gisuId = 9L;
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"))
        );
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter));
        given(manageChallengerUseCase.createChallengerBulk(any())).willReturn(List.of(1L));

        // When
        SeedChallengersResult result = sut.seed(new SeedChallengersCommand(gisuId, 1, null, null));

        // Then
        long expectedParts = java.util.Arrays.stream(ChallengerPart.values())
            .filter(p -> p != ChallengerPart.ADMIN).count();
        assertThat(result.perCellSummary()).hasSize((int) expectedParts);
        assertThat(result.perCellSummary())
            .noneMatch(s -> s.part() == ChallengerPart.ADMIN);
    }

    @Test
    @DisplayName("chapterIds 필터가 있으면 해당 Chapter 만 시딩한다")
    void chapterIds_필터_적용() {
        // Given
        Long gisuId = 9L;
        ChapterWithSchoolsInfo c1 = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"))
        );
        ChapterWithSchoolsInfo c2 = new ChapterWithSchoolsInfo(
            2L, "경기", List.of(new ChapterWithSchoolsInfo.SchoolInfo(201L, "성균관대"))
        );
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(c1, c2));
        given(manageChallengerUseCase.createChallengerBulk(any())).willReturn(List.of(1L));

        // When
        SeedChallengersResult result = sut.seed(new SeedChallengersCommand(
            gisuId, 1, List.of(ChallengerPart.WEB), List.of(2L)
        ));

        // Then
        assertThat(result.perCellSummary())
            .hasSize(1)
            .allSatisfy(s -> {
                assertThat(s.chapterId()).isEqualTo(2L);
                assertThat(s.schoolId()).isEqualTo(201L);
                assertThat(s.part()).isEqualTo(ChallengerPart.WEB);
            });
    }

    @Test
    @DisplayName("한 셀에서 createChallengerBulk 가 실패해도 다른 셀 시딩은 진행된다")
    void 셀_실패_격리() {
        // Given
        Long gisuId = 9L;
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(
            new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"),
            new ChapterWithSchoolsInfo.SchoolInfo(102L, "동국대")
        )
        );
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter));
        given(manageChallengerUseCase.createChallengerBulk(any()))
            .willThrow(new RuntimeException("boom on first cell"))
            .willReturn(List.of(99L));

        // When
        SeedChallengersResult result = sut.seed(new SeedChallengersCommand(
            gisuId, 1, List.of(ChallengerPart.WEB), null
        ));

        // Then
        assertThat(result.perCellSummary()).hasSize(2);
        assertThat(result.totalCreated()).isEqualTo(1);
        verify(manageChallengerUseCase, times(2)).createChallengerBulk(any());
    }

    @Test
    @DisplayName("멤버 batch 생성 실패는 memberFailed 로 보고되고 챌린저 생성은 호출하지 않는다")
    void 멤버_batch_실패_보고() {
        // Given
        Long gisuId = 9L;
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"))
        );
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter));
        org.mockito.Mockito.doThrow(new RuntimeException("member boom"))
            .when(registerEmailMemberUseCase).batchRegister(any());

        // When
        SeedChallengersResult result = sut.seed(new SeedChallengersCommand(
            gisuId, 2, List.of(ChallengerPart.WEB), null
        ));

        // Then
        assertThat(result.perCellSummary()).hasSize(1);
        SeedChallengersResult.PerCellSummary cell = result.perCellSummary().get(0);
        assertThat(cell.created()).isZero();
        assertThat(cell.memberFailed()).isEqualTo(2);
        assertThat(cell.challengerFailed()).isZero();
        assertThat(cell.totalFailed()).isEqualTo(2);
    }

    @Test
    @DisplayName("gisuId 가 null 이면 활성 기수를 사용한다")
    void gisuId_null_시_활성_기수_조회() {
        // Given
        given(getGisuUseCase.getActiveGisuId()).willReturn(10L);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(10L)).willReturn(List.of());

        // When
        SeedChallengersResult result = sut.seed(new SeedChallengersCommand(
            null, 1, List.of(ChallengerPart.WEB), null
        ));

        // Then
        assertThat(result.gisuId()).isEqualTo(10L);
    }

    private static List<Long> nextIds(AtomicLong sequence, int count) {
        List<Long> ids = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(sequence.getAndIncrement());
        }
        return ids;
    }

    @Test
    @DisplayName("Track 목록을 생략하면 학교별 기본 네 Track의 챌린저를 생성한다")
    void seedDefaultTracks() {
        // Given
        given(getGisuUseCase.getById(11L)).willReturn(
            new GisuInfo(11L, 11L, null, null, true, GisuLearningType.TRACK));
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(11L)).willReturn(List.of(
            new ChapterWithSchoolsInfo(1L, "지부", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "학교")))));
        given(manageChallengerUseCase.createChallengerBulk(any())).willReturn(List.of(99L));

        // When
        SeedChallengersResult result = sut.seed(new SeedChallengersCommand(11L, null, null, null, 1, null));

        // Then
        assertThat(result.totalCreated()).isEqualTo(4);
        assertThat(result.perCellSummary()).extracting(SeedChallengersResult.PerCellSummary::track)
            .containsExactly(ChallengerTrack.PLAN, ChallengerTrack.DESIGN,
                ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
        ArgumentCaptor<List<CreateChallengerCommand>> commands = ArgumentCaptor.forClass(List.class);
        verify(manageChallengerUseCase, times(4)).createChallengerBulk(commands.capture());
        assertThat(commands.getAllValues()).allSatisfy(batch -> {
            assertThat(batch).hasSize(1);
            assertThat(batch.getFirst().part()).isNull();
            assertThat(batch.getFirst().tracks()).hasSize(1).doesNotContain(ChallengerTrack.INFRA_PLUS);
        });
    }

    @Test
    @DisplayName("Track 기수에 Part를 요청하면 회원을 생성하기 전에 거부한다")
    void rejectPartBeforeCreatingMembers() {
        // Given
        given(getGisuUseCase.getById(11L)).willReturn(
            new GisuInfo(11L, 11L, null, null, true, GisuLearningType.TRACK));

        // When & Then
        assertThatThrownBy(() -> sut.seed(new SeedChallengersCommand(
            11L, 1, List.of(ChallengerPart.WEB), null)))
            .isInstanceOf(CommonException.class);
        verifyNoInteractions(registerEmailMemberUseCase, manageChallengerUseCase, dummyMemberFactory);
    }

    @Test
    @DisplayName("PLUS 시딩은 회원을 생성하기 전에 거부한다")
    void rejectPlusBeforeCreatingMembers() {
        // Given
        given(getGisuUseCase.getById(11L)).willReturn(
            new GisuInfo(11L, 11L, null, null, true, GisuLearningType.TRACK));

        // When & Then
        assertThatThrownBy(() -> sut.seed(new SeedChallengersCommand(
            11L, null, null, null, 1, List.of(ChallengerTrack.INFRA_PLUS))))
            .isInstanceOf(CommonException.class);
        verifyNoInteractions(registerEmailMemberUseCase, manageChallengerUseCase, dummyMemberFactory);
    }

    @Test
    @DisplayName("Track 기수의 학교별 생성 수가 없으면 회원을 생성하기 전에 거부한다")
    void rejectMissingTrackCountBeforeCreatingMembers() {
        // Given
        given(getGisuUseCase.getById(11L)).willReturn(
            new GisuInfo(11L, 11L, null, null, true, GisuLearningType.TRACK));

        // When & Then
        assertThatThrownBy(() -> sut.seed(new SeedChallengersCommand(
            11L, null, null, null, null, List.of(ChallengerTrack.PLAN))))
            .isInstanceOf(CommonException.class);
        verifyNoInteractions(registerEmailMemberUseCase, manageChallengerUseCase, dummyMemberFactory);
    }
}
