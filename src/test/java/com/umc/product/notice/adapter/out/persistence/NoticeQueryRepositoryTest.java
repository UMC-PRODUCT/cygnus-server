package com.umc.product.notice.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeClassification;
import com.umc.product.notice.domain.NoticeImage;
import com.umc.product.notice.domain.NoticeLink;
import com.umc.product.notice.domain.NoticeRead;
import com.umc.product.notice.domain.NoticeTarget;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@ActiveProfiles("test")
@Import({NoticeQueryRepository.class, NoticeContentsQueryRepository.class})
@DisplayName("Notice QueryDSL persistence 통합 테스트")
class NoticeQueryRepositoryTest {

    private static final PageRequest PAGE = PageRequest.of(0, 30);

    @Autowired
    TestEntityManager em;

    @Autowired
    NoticeQueryRepository queryRepository;

    @Autowired
    NoticeContentsQueryRepository contentsQueryRepository;

    @Test
    @DisplayName("챌린저 분류는 전체·지부·학교·파트 조합과 잘못된 조합을 구분한다")
    void challenger_classification_covers_all_scope_combinations() {
        Notice global = persistNotice("전체 공지", 1L, null, null, List.of(), NoticeTab.CHALLENGER);
        Notice chapter = persistNotice("지부 공지", 1L, 2L, null, List.of(ChallengerPart.WEB), NoticeTab.CHALLENGER);
        Notice school = persistNotice("학교 공지", 1L, null, 3L, List.of(ChallengerPart.WEB), NoticeTab.CHALLENGER);
        Notice part = persistNotice("파트 공지", 1L, null, null,
            List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER);
        flushAndClear();

        NoticeViewerInfo challenger = viewer(Set.of(ChallengerPart.WEB), null);
        assertThat(queryRepository.findByClassification(
            classification(1L, null, null, null, NoticeTab.CHALLENGER), challenger, PAGE
        )).extracting(Notice::getId).containsExactly(global.getId());
        assertThat(queryRepository.findByClassification(
            classification(1L, 2L, null, null, NoticeTab.CHALLENGER), challenger, PAGE
        )).extracting(Notice::getId).contains(chapter.getId());
        assertThat(queryRepository.findByClassification(
            classification(1L, null, 3L, null, NoticeTab.CHALLENGER), challenger, PAGE
        )).extracting(Notice::getId).contains(school.getId());

        NoticeClassification partInChapterAndSchool = classification(
            1L, 2L, 3L, ChallengerPart.SPRINGBOOT, NoticeTab.CHALLENGER
        );
        assertThat(queryRepository.findByClassification(partInChapterAndSchool, challenger, PAGE))
            .extracting(Notice::getId)
            .contains(part.getId());

        NoticeViewerInfo schoolCore = viewer(Set.of(), NoticeTab.SCHOOL_CORE);
        assertThat(queryRepository.findByClassification(
            classification(1L, 2L, null, null, NoticeTab.CHALLENGER), schoolCore, PAGE
        )).extracting(Notice::getId).contains(chapter.getId());

        assertThatThrownBy(() -> queryRepository.findByClassification(
            classification(null, null, null, null, NoticeTab.CHALLENGER), challenger, PAGE
        )).isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> queryRepository.findByClassification(
            classification(1L, 2L, 3L, null, NoticeTab.CHALLENGER), challenger, PAGE
        )).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("운영진 분류는 역할 하한·학교·명시 파트·담당 파트와 빈 담당 파트를 적용한다")
    void staff_classification_covers_role_school_and_part_conditions() {
        Notice central = persistNotice("중앙 공지", 1L, null, null, List.of(), NoticeTab.CENTRAL_MEMBER);
        Notice school = persistNotice("교내 공지", 1L, null, 3L, List.of(), NoticeTab.SCHOOL_CORE);
        Notice spring = persistNotice("스프링 파트장 공지", 1L, null, 3L,
            List.of(ChallengerPart.SPRINGBOOT), NoticeTab.SCHOOL_PART_LEADER);
        Notice web = persistNotice("웹 파트장 공지", 1L, null, 3L,
            List.of(ChallengerPart.WEB), NoticeTab.SCHOOL_PART_LEADER);
        flushAndClear();

        assertThat(queryRepository.findByClassification(
            classification(1L, null, null, null, NoticeTab.CENTRAL_MEMBER),
            viewer(Set.of(), NoticeTab.CENTRAL_MEMBER), PAGE
        )).extracting(Notice::getId).contains(central.getId());

        assertThat(queryRepository.findByClassification(
            classification(1L, null, 3L, null, NoticeTab.SCHOOL_CORE),
            viewer(Set.of(), NoticeTab.SCHOOL_CORE), PAGE
        )).extracting(Notice::getId).contains(school.getId(), spring.getId(), web.getId());

        assertThat(queryRepository.findByClassification(
            classification(1L, null, 3L, null, NoticeTab.SCHOOL_PART_LEADER),
            viewer(Set.of(ChallengerPart.SPRINGBOOT), NoticeTab.SCHOOL_PART_LEADER), PAGE
        )).extracting(Notice::getId).contains(spring.getId()).doesNotContain(web.getId());

        assertThat(queryRepository.findByClassification(
            classification(1L, null, 3L, ChallengerPart.WEB, NoticeTab.SCHOOL_PART_LEADER),
            viewer(Set.of(ChallengerPart.SPRINGBOOT), NoticeTab.SCHOOL_PART_LEADER), PAGE
        )).extracting(Notice::getId).containsExactly(web.getId());

        assertThat(queryRepository.findByClassification(
            classification(1L, null, 3L, null, NoticeTab.SCHOOL_PART_LEADER),
            viewer(null, NoticeTab.SCHOOL_PART_LEADER), PAGE
        )).extracting(Notice::getId).doesNotContain(spring.getId(), web.getId());
    }

    @Test
    @DisplayName("keyword·읽음 집계·미열람 조회·콘텐츠 표시 순서는 빈 입력과 실제 SQL 결과를 보장한다")
    void keyword_read_aggregate_unread_and_content_order_are_persisted() {
        Notice first = persistNotice("Spring 공지", 1L, null, null, List.of(), NoticeTab.CHALLENGER);
        Notice second = persistNotice("다른 제목", 1L, null, null, List.of(), NoticeTab.CHALLENGER);
        Challenger readChallenger = em.persist(new Challenger(101L, ChallengerPart.WEB, 1L));
        Challenger unreadChallenger = em.persist(new Challenger(102L, ChallengerPart.WEB, 1L));
        em.persist(NoticeRead.builder().notice(first).challengerId(readChallenger.getId()).build());
        em.persist(NoticeImage.create("image-0", first, 0));
        em.persist(NoticeImage.create("image-2", first, 2));
        em.persist(NoticeLink.create("https://one.example", first, 1));
        flushAndClear();

        NoticeClassification classification = classification(1L, null, null, null, NoticeTab.CHALLENGER);
        NoticeViewerInfo viewer = viewer(Set.of(ChallengerPart.WEB), null);
        assertThat(queryRepository.findByKeyword("Spring", classification, viewer, PAGE))
            .extracting(Notice::getId).containsExactly(first.getId());
        assertThat(queryRepository.findByKeyword(" ", classification, viewer, PAGE))
            .extracting(Notice::getId).contains(first.getId(), second.getId());
        assertThat(queryRepository.findByKeyword(null, classification, viewer, PAGE))
            .extracting(Notice::getId).contains(first.getId(), second.getId());

        assertThat(queryRepository.countReadsByNoticeIds(null)).isEmpty();
        assertThat(queryRepository.countReadsByNoticeIds(List.of())).isEmpty();
        assertThat(queryRepository.countReadsByNoticeIds(List.of(first.getId(), second.getId())))
            .containsEntry(first.getId(), 1L)
            .doesNotContainKey(second.getId());
        assertThat(queryRepository.findUnreadChallengerIdByNoticeId(first.getId()))
            .contains(unreadChallenger.getId())
            .doesNotContain(readChallenger.getId());

        assertThat(contentsQueryRepository.findNextImageDisplayOrder(first.getId())).isEqualTo(3);
        assertThat(contentsQueryRepository.findNextLinkDisplayOrder(first.getId())).isEqualTo(2);
        assertThat(contentsQueryRepository.findNextImageDisplayOrder(second.getId())).isZero();
        assertThat(contentsQueryRepository.findNextLinkDisplayOrder(second.getId())).isZero();
    }

    private Notice persistNotice(
        String title,
        Long gisuId,
        Long chapterId,
        Long schoolId,
        List<ChallengerPart> parts,
        NoticeTab tab
    ) {
        Notice notice = em.persist(Notice.create(title, title + " 내용", 10L, false, false));
        em.persist(NoticeTarget.builder()
            .noticeId(notice.getId())
            .targetGisuId(gisuId)
            .targetChapterId(chapterId)
            .targetSchoolId(schoolId)
            .targetChallengerPart(parts)
            .targetNoticeTab(tab)
            .build());
        return notice;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private static NoticeClassification classification(
        Long gisuId,
        Long chapterId,
        Long schoolId,
        ChallengerPart part,
        NoticeTab tab
    ) {
        return new NoticeClassification(gisuId, chapterId, schoolId, part, tab);
    }

    private static NoticeViewerInfo viewer(Set<ChallengerPart> parts, NoticeTab tab) {
        return new NoticeViewerInfo(parts, 3L, 2L, tab);
    }
}
