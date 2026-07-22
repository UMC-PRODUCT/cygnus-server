package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.application.port.in.command.ManageNoticeUseCase;
import com.umc.product.notice.application.port.in.command.dto.CreateNoticeCommand;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.test.application.port.in.command.dto.SeedNoticeCommand;
import com.umc.product.test.application.port.in.command.dto.SeedNoticeResult;
import com.umc.product.test.application.port.in.command.dto.SeedNoticeResult.ScopeSummary;

@ExtendWith(MockitoExtension.class)
class NoticeSeedServiceTest {

    @Mock
    DummyNoticeFactory dummyNoticeFactory;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    ManageNoticeUseCase manageNoticeUseCase;

    @InjectMocks
    NoticeSeedService sut;

    @BeforeEach
    void setUp() {
        lenient().when(dummyNoticeFactory.nextGlobalNoticeCommand(anyLong(), anyLong(), anyInt()))
            .thenAnswer(inv -> globalCommand(inv.getArgument(0), inv.getArgument(1)));
        lenient().when(dummyNoticeFactory.nextChapterNoticeCommand(anyLong(), anyLong(), anyLong(), any(), anyInt()))
            .thenAnswer(inv -> chapterCommand(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
        lenient().when(dummyNoticeFactory.nextSchoolNoticeCommand(anyLong(), anyLong(), anyLong(), any(), anyInt()))
            .thenAnswer(inv -> schoolCommand(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
        lenient().when(dummyNoticeFactory.nextPartNoticeCommand(anyLong(), anyLong(), any(), anyInt()))
            .thenAnswer(inv -> partCommand(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
    }

    @Test
    @DisplayName("4 가지 scope 별로 요청 수량만큼 공지가 생성된다")
    void scope별_수량_확인() {
        // Given - 2 chapter × 2 school each, 3 parts
        Long gisuId = 9L;
        Long authorMemberId = 100L;
        givenChapterWithSchools(gisuId);
        givenNoticeIdCounter();

        // When - GLOBAL 3, CHAPTER 2, SCHOOL 1, PART 2
        SeedNoticeResult result = sut.seed(new SeedNoticeCommand(
            gisuId, authorMemberId,
            3, 2, 1, 2,
            List.of(ChallengerPart.WEB, ChallengerPart.SPRINGBOOT)
        ));

        // Then - GLOBAL=3, CHAPTER=2*2=4, SCHOOL=2*2*1=4, PART=2*2=4 → 15
        assertThat(result.totalCreated()).isEqualTo(15);
        assertThat(result.createdNoticeIds()).hasSize(15);
        ScopeSummary global = findScope(result, "GLOBAL");
        ScopeSummary chapter = findScope(result, "CHAPTER");
        ScopeSummary school = findScope(result, "SCHOOL");
        ScopeSummary part = findScope(result, "PART");
        assertThat(global.created()).isEqualTo(3);
        assertThat(chapter.created()).isEqualTo(4);
        assertThat(school.created()).isEqualTo(4);
        assertThat(part.created()).isEqualTo(4);
    }

    @Test
    @DisplayName("권한 부족 등의 실패는 scope 단위 failed 카운트에 반영되고 다른 scope 는 진행된다")
    void scope_실패_격리() {
        // Given - CHAPTER scope 만 실패하도록 설정 (예: 권한 부족)
        Long gisuId = 9L;
        Long authorMemberId = 100L;
        givenChapterWithSchools(gisuId);
        AtomicLong idSeq = new AtomicLong(1000L);
        given(manageNoticeUseCase.createNotice(any())).willAnswer(inv -> {
            CreateNoticeCommand cmd = inv.getArgument(0);
            if (cmd.targetInfo().targetChapterId() != null) {
                throw new RuntimeException("NO_WRITE_PERMISSION for chapter");
            }
            return idSeq.getAndIncrement();
        });

        // When
        SeedNoticeResult result = sut.seed(new SeedNoticeCommand(
            gisuId, authorMemberId,
            1, 1, 1, 1,
            List.of(ChallengerPart.WEB)
        ));

        // Then
        ScopeSummary chapter = findScope(result, "CHAPTER");
        assertThat(chapter.failed()).isEqualTo(2); // 2 chapters × 1
        assertThat(chapter.created()).isZero();
        assertThat(findScope(result, "GLOBAL").created()).isEqualTo(1);
        assertThat(findScope(result, "SCHOOL").created()).isEqualTo(4); // 2 chapter × 2 school × 1
        assertThat(findScope(result, "PART").created()).isEqualTo(1);
    }

    @Test
    @DisplayName("모든 count 가 0 이면 createNotice 가 호출되지 않는다")
    void 모든_count_0_미호출() {
        // Given
        Long gisuId = 9L;
        givenChapterWithSchools(gisuId);

        // When
        SeedNoticeResult result = sut.seed(new SeedNoticeCommand(
            gisuId, 1L, 0, 0, 0, 0, null
        ));

        // Then
        assertThat(result.totalCreated()).isZero();
        verify(manageNoticeUseCase, never()).createNotice(any());
    }

    @Test
    @DisplayName("gisuId 가 null 이면 활성 기수를 사용한다")
    void gisuId_null_시_활성_기수() {
        // Given
        given(getGisuUseCase.getActiveGisuId()).willReturn(10L);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(10L)).willReturn(List.of());

        // When
        SeedNoticeResult result = sut.seed(new SeedNoticeCommand(
            null, 1L, 0, 0, 0, 0, null
        ));

        // Then
        assertThat(result.gisuId()).isEqualTo(10L);
        verify(getGisuUseCase, times(1)).getActiveGisuId();
    }

    @Test
    @DisplayName("parts 가 null 이면 ADMIN 제외 모든 파트가 사용된다")
    void parts_기본값_ADMIN_제외() {
        // Given
        Long gisuId = 9L;
        givenChapterWithSchools(gisuId);
        givenNoticeIdCounter();
        ArgumentCaptor<ChallengerPart> partCaptor = ArgumentCaptor.forClass(ChallengerPart.class);

        // When
        SeedNoticeResult result = sut.seed(new SeedNoticeCommand(
            gisuId, 1L, 0, 0, 0, 1, null
        ));

        // Then
        verify(dummyNoticeFactory, times((int) java.util.Arrays.stream(ChallengerPart.values())
            .filter(p -> p != ChallengerPart.ADMIN).count()))
            .nextPartNoticeCommand(anyLong(), anyLong(), partCaptor.capture(), anyInt());
        assertThat(partCaptor.getAllValues()).noneMatch(p -> p == ChallengerPart.ADMIN);
    }

    @Test
    @DisplayName("제목·내용에 대상 범위 정보가 포함된 채로 생성된다 (DummyNoticeFactory 위임 확인)")
    void 범위_정보_포함_위임() {
        // Given
        Long gisuId = 9L;
        givenChapterWithSchools(gisuId);
        givenNoticeIdCounter();
        ArgumentCaptor<CreateNoticeCommand> captor = ArgumentCaptor.forClass(CreateNoticeCommand.class);

        // When - GLOBAL 1, CHAPTER 1, SCHOOL 1, PART 1
        sut.seed(new SeedNoticeCommand(gisuId, 1L, 1, 1, 1, 1, List.of(ChallengerPart.WEB)));

        // Then
        verify(manageNoticeUseCase, times(8)).createNotice(captor.capture()); // 1 + 2 + 4 + 1
        List<CreateNoticeCommand> sent = captor.getAllValues();
        assertThat(sent).anyMatch(c -> c.title().contains("[전체]"));
        assertThat(sent).anyMatch(c -> c.title().contains("[지부]"));
        assertThat(sent).anyMatch(c -> c.title().contains("[학교]"));
        assertThat(sent).anyMatch(c -> c.title().contains("[파트]"));
    }

    @Test
    @DisplayName("모든 scope의 생성 실패를 각각 집계하고 ADMIN 파트는 제외한다")
    void 모든_scope_실패_집계() {
        Long gisuId = 9L;
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(
            new ChapterWithSchoolsInfo(1L, "서울", List.of(
                new ChapterWithSchoolsInfo.SchoolInfo(11L, "건국대")
            ))
        ));
        given(manageNoticeUseCase.createNotice(any()))
            .willThrow(new RuntimeException("permission denied"));

        SeedNoticeResult result = sut.seed(new SeedNoticeCommand(
            gisuId, 1L, 1, 1, 1, 1,
            List.of(ChallengerPart.ADMIN, ChallengerPart.WEB)
        ));

        assertThat(findScope(result, "GLOBAL").failed()).isOne();
        assertThat(findScope(result, "CHAPTER").failed()).isOne();
        assertThat(findScope(result, "SCHOOL").failed()).isOne();
        assertThat(findScope(result, "PART").failed()).isOne();
        verify(dummyNoticeFactory, never())
            .nextPartNoticeCommand(anyLong(), anyLong(), org.mockito.ArgumentMatchers.eq(ChallengerPart.ADMIN), anyInt());
    }

    private void givenChapterWithSchools(Long gisuId) {
        ChapterWithSchoolsInfo chapter1 = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(
            new ChapterWithSchoolsInfo.SchoolInfo(11L, "건국대"),
            new ChapterWithSchoolsInfo.SchoolInfo(12L, "동국대")
        )
        );
        ChapterWithSchoolsInfo chapter2 = new ChapterWithSchoolsInfo(
            2L, "경기", List.of(
            new ChapterWithSchoolsInfo.SchoolInfo(21L, "성균관대"),
            new ChapterWithSchoolsInfo.SchoolInfo(22L, "아주대")
        )
        );
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter1, chapter2));
    }

    private void givenNoticeIdCounter() {
        AtomicLong idSeq = new AtomicLong(1000L);
        given(manageNoticeUseCase.createNotice(any())).willAnswer(inv -> idSeq.getAndIncrement());
    }

    private ScopeSummary findScope(SeedNoticeResult result, String scope) {
        return result.scopeBreakdown().stream()
            .filter(s -> s.scope().equals(scope))
            .findFirst()
            .orElseThrow();
    }

    private CreateNoticeCommand globalCommand(Long gisuId, Long authorMemberId) {
        return new CreateNoticeCommand(
            authorMemberId, "[전체] g title", "[전체] g content", false, false,
            new NoticeTargetInfo(gisuId, null, null, List.of(), NoticeTab.CHALLENGER)
        );
    }

    private CreateNoticeCommand chapterCommand(Long gisuId, Long authorMemberId, Long chapterId) {
        return new CreateNoticeCommand(
            authorMemberId, "[지부] c title", "[지부] c content", false, false,
            new NoticeTargetInfo(gisuId, chapterId, null, List.of(), NoticeTab.CHALLENGER)
        );
    }

    private CreateNoticeCommand schoolCommand(Long gisuId, Long authorMemberId, Long schoolId) {
        return new CreateNoticeCommand(
            authorMemberId, "[학교] s title", "[학교] s content", false, false,
            new NoticeTargetInfo(gisuId, null, schoolId, List.of(), NoticeTab.CHALLENGER)
        );
    }

    private CreateNoticeCommand partCommand(Long gisuId, Long authorMemberId, ChallengerPart part) {
        return new CreateNoticeCommand(
            authorMemberId, "[파트] p title", "[파트] p content", false, false,
            new NoticeTargetInfo(gisuId, null, null, List.of(part), NoticeTab.CHALLENGER)
        );
    }
}
