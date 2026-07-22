package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.command.ManageCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookMissionUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateWeeklyCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ChangeOriginalWorkbookStatusCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateOriginalWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateOriginalWorkbookMissionCommand;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.test.application.port.in.command.dto.SeedCurriculumCommand;
import com.umc.product.test.application.port.in.command.dto.SeedCurriculumResult;

@ExtendWith(MockitoExtension.class)
class CurriculumSeedServiceTest {

    @Mock
    DummyCurriculumFactory dummyCurriculumFactory;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    ManageCurriculumUseCase manageCurriculumUseCase;
    @Mock
    ManageWeeklyCurriculumUseCase manageWeeklyCurriculumUseCase;
    @Mock
    ManageOriginalWorkbookUseCase manageOriginalWorkbookUseCase;
    @Mock
    ManageOriginalWorkbookMissionUseCase manageOriginalWorkbookMissionUseCase;

    @InjectMocks
    CurriculumSeedService sut;

    @BeforeEach
    void setUp() {
        lenient().when(dummyCurriculumFactory.nextCurriculumCommand(anyLong(), any()))
            .thenReturn(mock(CreateCurriculumCommand.class));
        lenient().when(dummyCurriculumFactory.nextWeeklyCurriculumCommand(anyLong(), anyLong()))
            .thenReturn(mock(CreateWeeklyCurriculumCommand.class));
        lenient().when(dummyCurriculumFactory.nextOriginalWorkbookCommand(anyLong(), anyLong()))
            .thenReturn(mock(CreateOriginalWorkbookCommand.class));
        lenient().when(dummyCurriculumFactory.nextOriginalWorkbookMissionCommand(anyLong(), anyInt()))
            .thenReturn(mock(CreateOriginalWorkbookMissionCommand.class));
    }

    @Test
    @DisplayName("기본 파트 사용 시 ADMIN 제외 모든 파트에 대해 커리큘럼이 생성된다")
    void 기본_파트_ADMIN_제외() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();

        // When
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(gisuId, 1, 0, null, null));

        // Then
        long expectedPartCount = java.util.Arrays.stream(ChallengerPart.values())
            .filter(p -> p != ChallengerPart.ADMIN).count();
        assertThat(result.createdCurriculumIds()).hasSize((int) expectedPartCount);
        verify(manageCurriculumUseCase, times((int) expectedPartCount)).create(any());
    }

    @Test
    @DisplayName("주차 수만큼 WeeklyCurriculum 과 OriginalWorkbook 이 생성된다")
    void 주차_수만큼_생성() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();

        // When - 2 파트 × 4 주차
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            gisuId, 4, 0, List.of(ChallengerPart.WEB, ChallengerPart.SPRINGBOOT), null
        ));

        // Then
        assertThat(result.createdCurriculumIds()).hasSize(2);
        assertThat(result.createdWeeklyCurriculumIds()).hasSize(8); // 2 × 4
        assertThat(result.createdOriginalWorkbookIds()).hasSize(8);
    }

    @Test
    @DisplayName("missionsPerWorkbook 만큼 미션이 생성된다")
    void 미션_수만큼_생성() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();

        // When - 1 파트 × 2 주차 × 3 미션
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            gisuId, 2, 3, List.of(ChallengerPart.WEB), null
        ));

        // Then
        assertThat(result.createdMissionIds()).hasSize(6); // 2 × 3
    }

    @Test
    @DisplayName("Curriculum 생성 실패는 다음 파트로 격리되어 다른 파트는 정상 시딩된다")
    void 파트별_실패_격리() {
        // Given
        Long gisuId = 9L;
        AtomicLong cId = new AtomicLong(100L);
        given(manageCurriculumUseCase.create(any()))
            .willThrow(new RuntimeException("first part boom"))
            .willAnswer(inv -> cId.getAndIncrement());
        AtomicLong wId = new AtomicLong(200L);
        given(manageWeeklyCurriculumUseCase.createBulk(any()))
            .willAnswer(inv -> nextIds(wId, inv.getArgument(0, List.class).size()));
        AtomicLong oId = new AtomicLong(300L);
        given(manageOriginalWorkbookUseCase.createBulk(any()))
            .willAnswer(inv -> nextIds(oId, inv.getArgument(0, List.class).size()));

        // When - 2 파트 시딩
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            gisuId, 2, 0, List.of(ChallengerPart.WEB, ChallengerPart.SPRINGBOOT), null
        ));

        // Then - 1번째 실패, 2번째 성공
        assertThat(result.curriculumFailed()).isEqualTo(1);
        assertThat(result.createdCurriculumIds()).hasSize(1);
        assertThat(result.createdWeeklyCurriculumIds()).hasSize(2); // 두 번째 파트의 2주차
    }

    @Test
    @DisplayName("releaseRequesterMemberId 가 지정되면 워크북을 RELEASED 로 전환한다")
    void release_요청_시_상태_전환() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();

        // When
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            gisuId, 1, 0, List.of(ChallengerPart.WEB), 999L
        ));

        // Then
        assertThat(result.released()).isTrue();
        verify(manageOriginalWorkbookUseCase, times(1)).changeStatusForRelease(any());
    }

    @Test
    @DisplayName("WeeklyCurriculum 과 OriginalWorkbook 은 파트별 bulk UseCase 로 생성한다")
    void 주차와_워크북_bulk_생성() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();

        // When
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            gisuId, 3, 0, List.of(ChallengerPart.WEB, ChallengerPart.SPRINGBOOT), null
        ));

        // Then
        assertThat(result.createdWeeklyCurriculumIds()).hasSize(6);
        assertThat(result.createdOriginalWorkbookIds()).hasSize(6);
        verify(manageWeeklyCurriculumUseCase, times(2)).createBulk(any());
        verify(manageOriginalWorkbookUseCase, times(2)).createBulk(any());
        verify(manageWeeklyCurriculumUseCase, never()).create(any());
        verify(manageOriginalWorkbookUseCase, never()).create(any());
    }

    @Test
    @DisplayName("releaseRequesterMemberId 가 null 이면 워크북 상태 전환을 호출하지 않는다")
    void release_미요청_시_상태_전환_안함() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();

        // When
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            gisuId, 1, 0, List.of(ChallengerPart.WEB), null
        ));

        // Then
        assertThat(result.released()).isFalse();
        verify(manageOriginalWorkbookUseCase, never()).changeStatusForRelease(any());
    }

    @Test
    @DisplayName("RELEASED 전환 실패 시 releaseFailed 에 카운트되고 released=false 로 반환한다")
    void release_실패_카운트() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();
        org.mockito.Mockito.doThrow(new RuntimeException("release boom"))
            .when(manageOriginalWorkbookUseCase).changeStatusForRelease(any());

        // When - 1 파트 × 2 주차 = 2 워크북
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            gisuId, 2, 0, List.of(ChallengerPart.WEB), 999L
        ));

        // Then
        assertThat(result.released()).isFalse();
        assertThat(result.releaseFailed()).isEqualTo(2);
    }

    @Test
    @DisplayName("RELEASED 전환 시 모든 워크북 ID 와 요청자 멤버 ID 가 정확히 전달된다")
    void release_명령_payload_확인() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();
        org.mockito.ArgumentCaptor<List<ChangeOriginalWorkbookStatusCommand>> captor =
            org.mockito.ArgumentCaptor.forClass(List.class);

        // When - 1 파트 × 3 주차 = 3 워크북
        sut.seed(new SeedCurriculumCommand(gisuId, 3, 0, List.of(ChallengerPart.WEB), 777L));

        // Then
        verify(manageOriginalWorkbookUseCase).changeStatusForRelease(captor.capture());
        List<ChangeOriginalWorkbookStatusCommand> sent = captor.getValue();
        assertThat(sent).hasSize(3);
        assertThat(sent).allSatisfy(cmd -> assertThat(cmd.requestedMemberId()).isEqualTo(777L));
    }

    @Test
    @DisplayName("gisuId 가 null 이면 활성 기수를 사용한다")
    void gisuId_null_시_활성_기수() {
        // Given
        given(getGisuUseCase.getActiveGisuId()).willReturn(10L);

        // When
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            null, 0, 0, List.of(), null
        ));

        // Then - parts 가 empty 라 기본 파트 사용, 0 주차로는 weekly 미생성 자체 검증
        assertThat(result.gisuId()).isEqualTo(10L);
        verify(getGisuUseCase, times(1)).getActiveGisuId();
    }

    @Test
    @DisplayName("parts 에 ADMIN 이 포함되어도 ADMIN 은 시딩되지 않는다")
    void ADMIN_은_필터링() {
        // Given
        Long gisuId = 9L;
        givenSequentialIds();

        // When
        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            gisuId, 1, 0, List.of(ChallengerPart.ADMIN, ChallengerPart.WEB), null
        ));

        // Then - WEB 1개만 생성
        assertThat(result.createdCurriculumIds()).hasSize(1);
    }

    @Test
    @DisplayName("주차·워크북 bulk와 미션 실패는 단계별 카운터로 격리된다")
    void 하위_단계별_실패_격리() {
        given(manageCurriculumUseCase.create(any())).willReturn(100L, 101L, 102L);
        given(manageWeeklyCurriculumUseCase.createBulk(any()))
            .willThrow(new RuntimeException("weekly boom"))
            .willReturn(List.of(201L), List.of(202L));
        given(manageOriginalWorkbookUseCase.createBulk(any()))
            .willThrow(new RuntimeException("workbook boom"))
            .willReturn(List.of(302L));
        given(manageOriginalWorkbookMissionUseCase.create(any()))
            .willThrow(new RuntimeException("mission boom"));

        SeedCurriculumResult result = sut.seed(new SeedCurriculumCommand(
            9L, 1, 1,
            List.of(ChallengerPart.WEB, ChallengerPart.IOS, ChallengerPart.SPRINGBOOT),
            null
        ));

        assertThat(result.weeklyCurriculumFailed()).isOne();
        assertThat(result.originalWorkbookFailed()).isOne();
        assertThat(result.missionFailed()).isOne();
        assertThat(result.createdOriginalWorkbookIds()).containsExactly(302L);
    }

    private void givenSequentialIds() {
        AtomicLong cId = new AtomicLong(100L);
        AtomicLong wId = new AtomicLong(200L);
        AtomicLong oId = new AtomicLong(300L);
        AtomicLong mId = new AtomicLong(400L);
        given(manageCurriculumUseCase.create(any())).willAnswer(inv -> cId.getAndIncrement());
        given(manageWeeklyCurriculumUseCase.createBulk(any()))
            .willAnswer(inv -> nextIds(wId, inv.getArgument(0, List.class).size()));
        given(manageOriginalWorkbookUseCase.createBulk(any()))
            .willAnswer(inv -> nextIds(oId, inv.getArgument(0, List.class).size()));
        lenient().when(manageOriginalWorkbookMissionUseCase.create(any()))
            .thenAnswer(inv -> mId.getAndIncrement());
    }

    private static List<Long> nextIds(AtomicLong sequence, int count) {
        List<Long> ids = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(sequence.getAndIncrement());
        }
        return ids;
    }
}
