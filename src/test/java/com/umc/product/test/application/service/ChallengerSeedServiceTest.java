package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.umc.product.member.application.port.in.command.RegisterEmailMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerCommand;
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

    @Test
    @DisplayName("셀 수량이 0이면 멤버와 챌린저 생성을 모두 생략한다")
    void 셀_수량_0_처리() {
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(9L)).willReturn(List.of(
            new ChapterWithSchoolsInfo(1L, "서울", List.of(
                new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대")
            ))
        ));

        SeedChallengersResult result = sut.seed(new SeedChallengersCommand(
            9L, 0, List.of(ChallengerPart.WEB), null
        ));

        assertThat(result.perCellSummary()).singleElement()
            .satisfies(cell -> assertThat(cell.totalFailed()).isZero());
        verify(registerEmailMemberUseCase, times(0)).batchRegister(any());
        verify(manageChallengerUseCase, times(0)).createChallengerBulk(any());
    }

    @Test
    @DisplayName("멤버 batch가 빈 목록을 반환하면 전원 실패로 집계하고 챌린저 생성을 생략한다")
    void 빈_멤버_batch_결과() {
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(9L)).willReturn(List.of(
            new ChapterWithSchoolsInfo(1L, "서울", List.of(
                new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대")
            ))
        ));
        org.mockito.Mockito.doReturn(List.of())
            .when(registerEmailMemberUseCase).batchRegister(any());

        SeedChallengersResult result = sut.seed(new SeedChallengersCommand(
            9L, 2, List.of(ChallengerPart.WEB), null
        ));

        assertThat(result.perCellSummary()).singleElement().satisfies(cell -> {
            assertThat(cell.created()).isZero();
            assertThat(cell.memberFailed()).isEqualTo(2);
        });
        verify(manageChallengerUseCase, times(0)).createChallengerBulk(any());
    }

    @Test
    @DisplayName("단건 챌린저 생성은 member·gisu·part를 그대로 위임한다")
    void 단건_챌린저_생성() {
        given(manageChallengerUseCase.createChallenger(any())).willReturn(100L);
        ArgumentCaptor<CreateChallengerCommand> captor =
            ArgumentCaptor.forClass(CreateChallengerCommand.class);

        var result = sut.create(CreateSeedChallengerCommand.of(
            1L, 9L, ChallengerPart.SPRINGBOOT
        ));

        verify(manageChallengerUseCase).createChallenger(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(1L);
        assertThat(captor.getValue().gisuId()).isEqualTo(9L);
        assertThat(captor.getValue().part()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(result.challengerId()).isEqualTo(100L);
        assertThat(result.memberId()).isEqualTo(1L);
    }

    private static List<Long> nextIds(AtomicLong sequence, int count) {
        List<Long> ids = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(sequence.getAndIncrement());
        }
        return ids;
    }
}
