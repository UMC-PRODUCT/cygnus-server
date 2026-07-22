package com.umc.product.notice.application.service.query;

import static com.umc.product.support.fixture.NoticeUnitFixture.notice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.notice.application.port.in.query.GetNoticeContentUseCase;
import com.umc.product.notice.application.port.in.query.dto.GetNoticeStatusQuery;
import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.LoadNoticeReadPort;
import com.umc.product.notice.application.port.out.LoadNoticeTargetPort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeClassification;
import com.umc.product.notice.domain.NoticeRead;
import com.umc.product.notice.domain.NoticeTarget;
import com.umc.product.notice.domain.enums.NoticeReadStatus;
import com.umc.product.notice.domain.enums.NoticeReadStatusFilterType;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Notice query service 테스트")
class NoticeQueryServiceTest {

    @Mock
    LoadNoticePort loadNoticePort;

    @Mock
    LoadNoticeReadPort loadNoticeReadPort;

    @Mock
    LoadNoticeTargetPort loadNoticeTargetPort;

    @Mock
    GetChapterUseCase getChapterUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetNoticeContentUseCase getNoticeContentUseCase;

    NoticeQueryService service;
    PageRequest pageable;

    @BeforeEach
    void setUp() {
        service = new NoticeQueryService(
            loadNoticePort,
            loadNoticeReadPort,
            loadNoticeTargetPort,
            getChapterUseCase,
            getMemberUseCase,
            getChallengerUseCase,
            getNoticeContentUseCase
        );
        pageable = PageRequest.of(0, 20);
    }

    @Test
    @DisplayName("공지 목록은 target과 작성자를 batch 조회하고 누락 연관 데이터는 null로 매핑한다")
    void 공지_목록은_연관_데이터를_batch로_조립한다() {
        Notice first = notice(1L, 10L);
        Notice second = notice(2L, 20L);
        NoticeClassification classification = classification(null, null, null, NoticeTab.CHALLENGER);
        NoticeViewerInfo viewer = viewer(3L, 2L, null);
        given(loadNoticePort.findNoticesByClassification(classification, viewer, pageable))
            .willReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        given(loadNoticeTargetPort.findByNoticeIdIn(List.of(1L, 2L)))
            .willReturn(List.of(target(1L, 1L, null, null, NoticeTab.CHALLENGER)));
        given(getMemberUseCase.findAllByIds(Set.of(10L, 20L)))
            .willReturn(Map.of(10L, member(10L, 3L)));

        var result = service.getAllNoticeSummaries(viewer, classification, pageable);

        assertThat(result).hasSize(2);
        assertThat(result.getContent().get(0).targetInfo()).isNotNull();
        assertThat(result.getContent().get(0).authorNickname()).isEqualTo("nickname-10");
        assertThat(result.getContent().get(1).targetInfo()).isNull();
        assertThat(result.getContent().get(1).authorNickname()).isNull();
        assertThat(result.getContent().get(1).authorName()).isNull();
    }

    @Test
    @DisplayName("파트 검색은 challenger의 지부·학교를 보완하고 keyword repository를 호출한다")
    void 파트_검색은_조회자_scope를_보완한다() {
        NoticeClassification input = classification(null, null, ChallengerPart.SPRINGBOOT, NoticeTab.CHALLENGER);
        NoticeClassification enriched = new NoticeClassification(
            1L, 2L, 3L, ChallengerPart.SPRINGBOOT, NoticeTab.CHALLENGER
        );
        NoticeViewerInfo viewer = viewer(3L, 2L, null);
        given(loadNoticePort.findNoticesByKeyword("spring", enriched, viewer, pageable))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));
        given(loadNoticeTargetPort.findByNoticeIdIn(List.of())).willReturn(List.of());
        given(getMemberUseCase.findAllByIds(Set.of())).willReturn(Map.of());

        assertThat(service.searchNoticesByKeyword("spring", viewer, input, pageable)).isEmpty();
    }

    @Test
    @DisplayName("운영진 목록은 중앙 역할이 요청 역할 이상일 때 조회한다")
    void 운영진_목록은_역할_계층을_검증한다() {
        NoticeClassification classification = classification(null, 3L, null, NoticeTab.SCHOOL_CORE);
        NoticeViewerInfo viewer = viewer(99L, 99L, NoticeTab.CENTRAL_MEMBER);
        given(loadNoticePort.findNoticesByClassification(classification, viewer, pageable))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));
        given(loadNoticeTargetPort.findByNoticeIdIn(List.of())).willReturn(List.of());
        given(getMemberUseCase.findAllByIds(Set.of())).willReturn(Map.of());

        assertThat(service.getAllNoticeSummaries(viewer, classification, pageable)).isEmpty();
    }

    @Test
    @DisplayName("다른 지부·학교와 권한 없는 운영진 조회는 fail-closed로 거부한다")
    void 조회_scope와_역할을_fail_closed로_검증한다() {
        NoticeViewerInfo challenger = viewer(3L, 2L, null);
        assertThatThrownBy(() -> service.getAllNoticeSummaries(
            challenger, classification(4L, null, null, NoticeTab.CHALLENGER), pageable
        )).isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> service.getAllNoticeSummaries(
            challenger, classification(null, 4L, null, NoticeTab.CHALLENGER), pageable
        )).isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> service.getAllNoticeSummaries(
            challenger, classification(null, null, null, NoticeTab.SCHOOL_CORE), pageable
        )).isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> service.getAllNoticeSummaries(
            viewer(3L, 2L, NoticeTab.SCHOOL_PART_LEADER),
            classification(null, null, null, NoticeTab.SCHOOL_CORE),
            pageable
        )).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("공지 상세는 부가 콘텐츠와 target을 조립하고 target 누락을 허용한다")
    void 공지_상세를_조립한다() {
        Notice targetNotice = notice();
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(targetNotice));
        given(getNoticeContentUseCase.findImageByNoticeId(1L)).willReturn(List.of());
        given(getNoticeContentUseCase.findLinkByNoticeId(1L)).willReturn(List.of());
        given(getNoticeContentUseCase.findVoteByNoticeId(1L, 10L)).willReturn(null);
        given(loadNoticeTargetPort.findByNoticeId(1L))
            .willReturn(Optional.of(target(1L, 1L, null, null, NoticeTab.CHALLENGER)), Optional.empty());

        assertThat(service.getNoticeDetail(1L, 10L).targetInfo()).isNotNull();
        assertThat(service.getNoticeDetail(1L, 10L).targetInfo()).isNull();

        given(loadNoticePort.findNoticeById(2L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getNoticeDetail(2L, 10L)).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("UNREAD 현황은 20개 page와 challenger ID cursor를 만들고 다음 페이지를 표시한다")
    void UNREAD_현황은_cursor_page를_계산한다() {
        List<ChallengerInfo> challengers = LongStream.rangeClosed(1, 22)
            .mapToObj(id -> challenger(id, id + 100, 1L, ChallengerPart.SPRINGBOOT))
            .toList();
        givenTargetContext(1L, challengers, fullMemberMap(challengers), true);
        given(loadNoticeReadPort.findNoticeReadByNoticeId(1L)).willReturn(List.of());

        var result = service.getReadStatus(new GetNoticeStatusQuery(
            null, 1L, NoticeReadStatusFilterType.ALL, null, NoticeReadStatus.UNREAD
        ));

        assertThat(result.content()).hasSize(20);
        assertThat(result.cursorId()).isEqualTo(20L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.content().getFirst().chapterName()).isEqualTo("지부");
    }

    @Test
    @DisplayName("READ 현황은 학교 filter와 NoticeRead ID cursor 다음부터 반환한다")
    void READ_현황은_read_ID_cursor를_사용한다() {
        List<ChallengerInfo> challengers = List.of(
            challenger(1L, 101L, 1L, ChallengerPart.WEB),
            challenger(2L, 102L, 1L, ChallengerPart.WEB),
            challenger(3L, 103L, 1L, ChallengerPart.WEB)
        );
        givenTargetContext(1L, challengers, fullMemberMap(challengers), false);
        List<NoticeRead> reads = List.of(read(100L, 1L), read(101L, 2L), read(102L, 3L));
        given(loadNoticeReadPort.findNoticeReadByNoticeId(1L)).willReturn(reads);

        var result = service.getReadStatus(new GetNoticeStatusQuery(
            100L, 1L, NoticeReadStatusFilterType.SCHOOL, List.of(3L), NoticeReadStatus.READ
        ));

        assertThat(result.content()).extracting(info -> info.challengerId()).containsExactly(2L, 3L);
        assertThat(result.cursorId()).isEqualTo(102L);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("현황은 대상 또는 조직 결과가 비면 외부 read 조회를 단축한다")
    void 현황은_빈_대상과_조직을_단축한다() {
        given(loadNoticeTargetPort.findByNoticeId(1L))
            .willReturn(Optional.of(target(1L, 1L, null, null, NoticeTab.CHALLENGER)));
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(List.of());
        assertThat(service.getReadStatus(new GetNoticeStatusQuery(
            null, 1L, NoticeReadStatusFilterType.ALL, null, NoticeReadStatus.UNREAD
        )).content()).isEmpty();

        List<ChallengerInfo> challengers = List.of(challenger(1L, 101L, 1L, ChallengerPart.WEB));
        givenTargetContext(2L, challengers, fullMemberMap(challengers), true);
        assertThat(service.getReadStatus(new GetNoticeStatusQuery(
            null, 2L, NoticeReadStatusFilterType.CHAPTER, List.of(), NoticeReadStatus.UNREAD
        )).content()).isEmpty();
    }

    @Test
    @DisplayName("읽음 통계는 전체 기수 최신 challenger 대상과 0명 경계를 계산한다")
    void 읽음_통계는_대상자와_비율을_계산한다() {
        NoticeTarget allGisu = target(1L, null, null, null, NoticeTab.CHALLENGER);
        given(loadNoticeTargetPort.findByNoticeId(1L)).willReturn(Optional.of(allGisu));
        List<ChallengerInfo> challengers = List.of(
            challenger(1L, 101L, 1L, ChallengerPart.WEB),
            challenger(2L, 102L, 2L, ChallengerPart.SPRINGBOOT)
        );
        given(getChallengerUseCase.getAllLatestGisuPerMemberWithoutChallengerPoints()).willReturn(challengers);
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(101L, 102L))).willReturn(Map.of(101L, 3L, 102L, 3L));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(1L, 2L), Set.of(3L)))
            .willReturn(Map.of());
        given(getChapterUseCase.byGisuAndSchool(1L, 3L)).willReturn(new ChapterInfo(4L, "지부"));
        given(getChapterUseCase.byGisuAndSchool(2L, 3L)).willReturn(new ChapterInfo(4L, "지부"));
        given(loadNoticeReadPort.countReadsByChallengerIdIn(1L, List.of(1L, 2L))).willReturn(1L);

        var result = service.getReadStatistics(1L);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.readCount()).isEqualTo(1);
        assertThat(result.unreadCount()).isEqualTo(1);
        assertThat(result.readRate()).isEqualTo(50F);

        given(loadNoticeTargetPort.findByNoticeId(2L))
            .willReturn(Optional.of(target(2L, 1L, null, null, NoticeTab.CHALLENGER)));
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(List.of());
        assertThat(service.getReadStatistics(2L).totalCount()).isZero();
    }

    @Test
    @DisplayName("현황과 통계는 target이 없으면 not-found로 거부한다")
    void target_누락을_거부한다() {
        given(loadNoticeTargetPort.findByNoticeId(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getReadStatus(new GetNoticeStatusQuery(
            null, 99L, NoticeReadStatusFilterType.ALL, null, NoticeReadStatus.UNREAD
        ))).isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> service.getReadStatistics(99L)).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("전체 기수 현황은 최신 challenger만 사용하고 회원·지부 누락을 안전하게 처리한다")
    void 전체_기수_현황은_누락된_회원과_지부를_안전하게_처리한다() {
        given(loadNoticeTargetPort.findByNoticeId(1L))
            .willReturn(Optional.of(target(1L, null, null, null, NoticeTab.CHALLENGER)));
        List<ChallengerInfo> challengers = List.of(
            challenger(1L, 101L, 1L, ChallengerPart.WEB),
            challenger(2L, 102L, null, ChallengerPart.SPRINGBOOT),
            challenger(3L, 103L, 1L, ChallengerPart.ANDROID)
        );
        given(getChallengerUseCase.getAllLatestGisuPerMemberWithoutChallengerPoints()).willReturn(challengers);
        given(getMemberUseCase.findAllByIds(Set.of(101L, 102L, 103L))).willReturn(Map.of(
            101L, member(101L, 3L),
            102L, member(102L, 3L)
        ));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(1L), Set.of(3L)))
            .willReturn(Map.of(1L, Map.of(3L, new ChapterInfo(4L, "지부"))));
        given(loadNoticeReadPort.findNoticeReadByNoticeId(1L)).willReturn(List.of());

        var result = service.getReadStatus(new GetNoticeStatusQuery(
            999L, 1L, NoticeReadStatusFilterType.ALL, null, NoticeReadStatus.UNREAD
        ));

        assertThat(result.content()).extracting(info -> info.challengerId()).containsExactly(1L, 2L);
        assertThat(result.content().get(1).chapterId()).isNull();
    }

    @Test
    @DisplayName("지부 현황 filter는 사전 조회한 지부를 사용하고 알 수 없는 cursor는 첫 페이지로 복구한다")
    void 지부_filter와_알_수_없는_cursor를_처리한다() {
        List<ChallengerInfo> challengers = List.of(
            challenger(1L, 101L, 1L, ChallengerPart.WEB),
            challenger(2L, 102L, 1L, ChallengerPart.SPRINGBOOT)
        );
        givenTargetContext(1L, challengers, fullMemberMap(challengers), true);
        given(loadNoticeReadPort.findNoticeReadByNoticeId(1L)).willReturn(List.of());

        var result = service.getReadStatus(new GetNoticeStatusQuery(
            999L, 1L, NoticeReadStatusFilterType.CHAPTER, List.of(4L), NoticeReadStatus.UNREAD
        ));
        var withoutFilter = service.getReadStatus(new GetNoticeStatusQuery(
            null, 1L, null, null, NoticeReadStatus.UNREAD
        ));

        assertThat(result.content()).extracting(info -> info.challengerId()).containsExactly(1L, 2L);
        assertThat(withoutFilter.content()).hasSize(2);
    }

    @Test
    @DisplayName("통계 대상은 중첩 지부 cache를 사용하고 학교가 누락된 회원은 제외한다")
    void 통계는_중첩_지부_cache와_학교_누락을_처리한다() {
        given(loadNoticeTargetPort.findByNoticeId(1L))
            .willReturn(Optional.of(target(1L, 1L, 4L, null, NoticeTab.CHALLENGER)));
        List<ChallengerInfo> challengers = List.of(
            challenger(1L, 101L, 1L, ChallengerPart.WEB),
            challenger(2L, 102L, 1L, ChallengerPart.WEB)
        );
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(challengers);
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(101L, 102L))).willReturn(Map.of(101L, 3L));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(1L), Set.of(3L)))
            .willReturn(Map.of(1L, Map.of(3L, new ChapterInfo(4L, "지부"))));
        given(loadNoticeReadPort.countReadsByChallengerIdIn(1L, List.of(1L))).willReturn(1L);

        var result = service.getReadStatistics(1L);

        assertThat(result.totalCount()).isOne();
        assertThat(result.readRate()).isEqualTo(100F);
    }

    @Test
    @DisplayName("명시한 지부 분류는 자동 보완하지 않고 그대로 repository에 전달한다")
    void 명시한_지부_분류를_보존한다() {
        NoticeClassification classification = new NoticeClassification(
            1L, 2L, null, ChallengerPart.SPRINGBOOT, NoticeTab.CHALLENGER
        );
        NoticeClassification enriched = new NoticeClassification(
            1L, 2L, 3L, ChallengerPart.SPRINGBOOT, NoticeTab.CHALLENGER
        );
        NoticeViewerInfo viewer = viewer(3L, 2L, null);
        given(loadNoticePort.findNoticesByClassification(enriched, viewer, pageable))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));
        given(loadNoticeTargetPort.findByNoticeIdIn(List.of())).willReturn(List.of());
        given(getMemberUseCase.findAllByIds(Set.of())).willReturn(Map.of());

        assertThat(service.getAllNoticeSummaries(viewer, classification, pageable)).isEmpty();
    }

    private void givenTargetContext(
        Long noticeId,
        List<ChallengerInfo> challengers,
        Map<Long, MemberInfo> members,
        boolean prefetchChapter
    ) {
        given(loadNoticeTargetPort.findByNoticeId(noticeId))
            .willReturn(Optional.of(target(noticeId, 1L, null, null, NoticeTab.CHALLENGER)));
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(challengers);
        Set<Long> memberIds = challengers.stream().map(ChallengerInfo::memberId).collect(java.util.stream.Collectors.toSet());
        given(getMemberUseCase.findAllByIds(memberIds)).willReturn(members);
        Set<Long> gisuIds = challengers.stream().map(ChallengerInfo::gisuId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Map<Long, ChapterInfo>> chapterMap = prefetchChapter
            ? Map.of(1L, Map.of(3L, new ChapterInfo(4L, "지부")))
            : Map.of();
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(gisuIds, Set.of(3L))).willReturn(chapterMap);
        given(getChapterUseCase.byGisuAndSchool(1L, 3L)).willReturn(new ChapterInfo(4L, "지부"));
    }

    private static NoticeClassification classification(
        Long chapterId,
        Long schoolId,
        ChallengerPart part,
        NoticeTab tab
    ) {
        return new NoticeClassification(1L, chapterId, schoolId, part, tab);
    }

    private static NoticeViewerInfo viewer(Long schoolId, Long chapterId, NoticeTab role) {
        return new NoticeViewerInfo(Set.of(ChallengerPart.SPRINGBOOT), schoolId, chapterId, role);
    }

    private static NoticeTarget target(
        Long noticeId,
        Long gisuId,
        Long chapterId,
        Long schoolId,
        NoticeTab tab
    ) {
        return NoticeTarget.builder()
            .noticeId(noticeId)
            .targetGisuId(gisuId)
            .targetChapterId(chapterId)
            .targetSchoolId(schoolId)
            .targetNoticeTab(tab)
            .build();
    }

    private static ChallengerInfo challenger(Long id, Long memberId, Long gisuId, ChallengerPart part) {
        return ChallengerInfo.builder().challengerId(id).memberId(memberId).gisuId(gisuId).part(part).build();
    }

    private static MemberInfo member(Long memberId, Long schoolId) {
        return MemberInfo.builder()
            .id(memberId)
            .name("member-" + memberId)
            .nickname("nickname-" + memberId)
            .profileImageLink("profile-" + memberId)
            .schoolId(schoolId)
            .schoolName("학교")
            .build();
    }

    private static Map<Long, MemberInfo> fullMemberMap(List<ChallengerInfo> challengers) {
        Map<Long, MemberInfo> result = new LinkedHashMap<>();
        challengers.forEach(challenger -> result.put(challenger.memberId(), member(challenger.memberId(), 3L)));
        return result;
    }

    private static NoticeRead read(Long id, Long challengerId) {
        NoticeRead read = NoticeRead.builder().notice(notice()).challengerId(challengerId).build();
        ReflectionTestUtils.setField(read, "id", id);
        return read;
    }
}
