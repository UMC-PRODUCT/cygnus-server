package com.umc.product.inhouse.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberSearchCondition;
import com.umc.product.inhouse.domain.UmcProductChapter;
import com.umc.product.inhouse.domain.UmcProductChapterMembership;
import com.umc.product.inhouse.domain.UmcProductDepartment;
import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.UmcProductLeadership;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberAccount;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;
import com.umc.product.member.domain.Member;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.LockModeType;

@PersistenceAdapterTest
@ActiveProfiles("test")
@Import({
    UmcProductMemberQueryRepository.class,
    UmcProductChapterPersistenceAdapter.class,
    UmcProductMemberPersistenceAdapter.class,
    UmcProductMemberAccountPersistenceAdapter.class,
    UmcProductMemberActivityPeriodPersistenceAdapter.class,
    UmcProductChapterMembershipPersistenceAdapter.class,
    UmcProductLeadershipPersistenceAdapter.class,
    UmcProductDepartmentPersistenceAdapter.class,
    UmcProductDepartmentParticipantPersistenceAdapter.class
})
class UmcProductPersistenceAdapterTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Autowired
    TestEntityManager em;

    @Autowired
    UmcProductChapterPersistenceAdapter chapterAdapter;

    @Autowired
    UmcProductMemberPersistenceAdapter memberAdapter;

    @Autowired
    UmcProductMemberAccountPersistenceAdapter accountAdapter;

    @Autowired
    UmcProductMemberActivityPeriodPersistenceAdapter activityPeriodAdapter;

    @Autowired
    UmcProductChapterMembershipPersistenceAdapter chapterMembershipAdapter;

    @Autowired
    UmcProductLeadershipPersistenceAdapter leadershipAdapter;

    @Autowired
    UmcProductDepartmentPersistenceAdapter departmentAdapter;

    @Autowired
    UmcProductDepartmentParticipantPersistenceAdapter departmentParticipantAdapter;

    @Test
    void 활동_기간의_LocalDate는_DATE로_그대로_왕복하고_종료일을_포함한다() {
        UmcProductMember member = saveMember(100L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, END_DATE);

        em.flush();
        em.clear();

        UmcProductMemberActivityPeriod reloaded = activityPeriodAdapter.getById(period.getId());

        assertThat(reloaded.getStartDate()).isEqualTo(START_DATE);
        assertThat(reloaded.getEndDate()).isEqualTo(END_DATE);
        assertThat(reloaded.isActiveOn(START_DATE)).isTrue();
        assertThat(reloaded.isActiveOn(END_DATE)).isTrue();
        assertThat(activityPeriodAdapter.findContaining(
            member.getId(),
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31)
        )).map(UmcProductMemberActivityPeriod::getId).contains(period.getId());
    }

    @Test
    void 한_인원에_여러_계정을_연동하고_계정으로_인원을_역조회한다() {
        UmcProductMember member = saveMember(120L);
        Member provisioned = saveLoginAccount("provisioned");
        Member linked = saveLoginAccount("linked");
        accountAdapter.save(UmcProductMemberAccount.create(
            member,
            provisioned.getId(),
            UmcProductMemberAccountType.PROVISIONED
        ));
        accountAdapter.save(UmcProductMemberAccount.create(
            member,
            linked.getId(),
            UmcProductMemberAccountType.LINKED
        ));

        em.flush();
        em.clear();

        assertThat(accountAdapter.listByUmcProductMemberId(member.getId())).hasSize(2);
        assertThat(accountAdapter.findByMemberId(provisioned.getId()))
            .map(account -> account.getUmcProductMember().getId())
            .contains(member.getId());
        assertThat(accountAdapter.existsByUmcProductMemberIdAndMemberId(member.getId(), linked.getId()))
            .isTrue();
    }

    @Test
    void 이미_연동된_계정을_다른_인원에_연동하면_도메인_충돌로_변환한다() {
        UmcProductMember first = saveMember(121L);
        UmcProductMember second = saveMember(122L);
        Member loginAccount = saveLoginAccount("duplicated");
        accountAdapter.save(UmcProductMemberAccount.create(
            first,
            loginAccount.getId(),
            UmcProductMemberAccountType.LINKED
        ));

        assertThatThrownBy(() -> accountAdapter.save(UmcProductMemberAccount.create(
            second,
            loginAccount.getId(),
            UmcProductMemberAccountType.LINKED
        )))
            .isInstanceOf(InhouseDomainException.class)
            .satisfies(exception -> assertThat(((InhouseDomainException) exception).getBaseCode())
                .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACCOUNT_ALREADY_LINKED));
    }

    @Test
    void Chapter_코드_중복_DB_제약은_도메인_충돌로_변환한다() {
        saveChapter("DUPLICATED_CHAPTER", 1, true);

        assertThatThrownBy(() -> saveChapter("DUPLICATED_CHAPTER", 2, true))
            .isInstanceOf(InhouseDomainException.class)
            .satisfies(exception -> assertThat(((InhouseDomainException) exception).getBaseCode())
                .isEqualTo(InhouseErrorCode.UMC_PRODUCT_CHAPTER_ALREADY_EXISTS));
    }

    @Test
    void Chapter를_비관적_쓰기_잠금으로_조회한다() {
        UmcProductChapter chapter = saveChapter("LOCKED_CHAPTER", 1, true);
        em.flush();
        em.clear();

        UmcProductChapter lockedChapter = chapterAdapter.getByIdWithLock(chapter.getId());

        assertThat(em.getEntityManager().getLockMode(lockedChapter))
            .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void 멤버_활동_기간은_겹치거나_빈틈없이_인접할_수_없다() {
        UmcProductMember member = saveMember(101L);
        saveActivityPeriod(member, START_DATE, LocalDate.of(2026, 3, 31));
        em.flush();

        assertThat(activityPeriodAdapter.existsOverlappingOrAdjacent(
            member.getId(),
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 6, 30),
            null
        )).isTrue();

        assertThatThrownBy(() -> {
            saveActivityPeriod(
                member,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30)
            );
            em.flush();
        }).isInstanceOf(InhouseDomainException.class)
            .satisfies(exception -> assertThat(((InhouseDomainException) exception).getBaseCode())
                .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED));
    }

    @Test
    void Chapter는_활성_필터와_정렬_순서를_유지한다() {
        UmcProductChapter activeChapter = saveChapter("DEVELOP", 2, true);
        saveChapter("DESIGN", 1, false);
        em.flush();
        em.clear();

        assertThat(chapterAdapter.listAll(true))
            .extracting(UmcProductChapter::getCode)
            .containsExactly("DEVELOP");
        assertThat(chapterAdapter.getById(activeChapter.getId()).getCode()).isEqualTo("DEVELOP");
    }

    @Test
    void 멤버는_같은_기간에_여러_Chapter에_소속될_수_있다() {
        UmcProductChapter developChapter = saveChapter("DEVELOP", 1, true);
        UmcProductChapter clientChapter = saveChapter("CLIENT", 2, true);
        UmcProductMember member = saveMember(102L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, null);

        chapterMembershipAdapter.save(UmcProductChapterMembership.create(
            period,
            developChapter,
            UmcProductPosition.SERVER_DEVELOPER,
            "API 개발",
            null,
            START_DATE,
            null
        ));
        chapterMembershipAdapter.save(UmcProductChapterMembership.create(
            period,
            clientChapter,
            UmcProductPosition.WEB_DEVELOPER,
            "웹 개발",
            null,
            START_DATE,
            null
        ));
        em.flush();
        em.clear();

        assertThat(chapterMembershipAdapter.listByUmcProductMemberId(member.getId()))
            .extracting(membership -> membership.getChapter().getCode())
            .containsExactlyInAnyOrder("DEVELOP", "CLIENT");
    }

    @Test
    void 멤버_검색의_activeOn은_멤버와_Chapter_소속_기간을_모두_검사한다() {
        UmcProductChapter chapter = saveChapter("ENGINEERING", 1, true);
        UmcProductMember currentMember = saveMember(109L);
        UmcProductMember pastMember = saveMember(110L);
        UmcProductMemberActivityPeriod currentPeriod = saveActivityPeriod(currentMember, START_DATE, null);
        UmcProductMemberActivityPeriod pastPeriod = saveActivityPeriod(
            pastMember,
            START_DATE,
            LocalDate.of(2026, 3, 31)
        );
        chapterMembershipAdapter.save(UmcProductChapterMembership.create(
            currentPeriod,
            chapter,
            UmcProductPosition.SERVER_DEVELOPER,
            "API 개발",
            null,
            START_DATE,
            null
        ));
        chapterMembershipAdapter.save(UmcProductChapterMembership.create(
            pastPeriod,
            chapter,
            UmcProductPosition.SERVER_DEVELOPER,
            "API 개발",
            null,
            START_DATE,
            LocalDate.of(2026, 3, 31)
        ));
        em.flush();

        UmcProductMemberSearchCondition condition = UmcProductMemberSearchCondition.of(
            chapter.getId(),
            null,
            UmcProductPosition.SERVER_DEVELOPER,
            null,
            LocalDate.of(2026, 4, 1)
        );

        assertThat(memberAdapter.searchIds(condition, PageRequest.of(0, 10)).getContent())
            .containsExactly(currentMember.getId());
    }

    @Test
    void 같은_멤버의_같은_Chapter_소속_기간은_겹칠_수_없다() {
        UmcProductChapter chapter = saveChapter("PRODUCT", 1, true);
        UmcProductMember first = saveMember(103L);
        UmcProductMemberActivityPeriod firstPeriod = saveActivityPeriod(first, START_DATE, END_DATE);

        chapterMembershipAdapter.save(UmcProductChapterMembership.create(
            firstPeriod,
            chapter,
            UmcProductPosition.PRODUCT_OWNER,
            "운영 리드",
            null,
            START_DATE,
            LocalDate.of(2026, 6, 30)
        ));
        em.flush();

        assertThat(chapterMembershipAdapter.existsOverlappingChapterMembership(
            first.getId(),
            chapter.getId(),
            LocalDate.of(2026, 6, 30),
            END_DATE,
            null
        )).isTrue();
        assertThatThrownBy(() -> {
            chapterMembershipAdapter.save(UmcProductChapterMembership.create(
                firstPeriod,
                chapter,
                UmcProductPosition.PRODUCT_OWNER,
                "후임 운영 리드",
                null,
                LocalDate.of(2026, 6, 30),
                END_DATE
            ));
            em.flush();
        }).isInstanceOf(InhouseDomainException.class)
            .satisfies(exception -> assertThat(((InhouseDomainException) exception).getBaseCode())
                .isEqualTo(InhouseErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED));
    }

    @Test
    void Leadership은_멤버_활동_기간과_자체_기간이_모두_유효한_날에만_조회된다() {
        UmcProductMember member = saveMember(105L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, END_DATE);
        leadershipAdapter.save(UmcProductLeadership.create(
            period,
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 8, 31)
        ));
        em.flush();

        Set<UmcProductLeadershipRole> managementRoles = Set.of(
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
            UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD
        );

        assertThat(leadershipAdapter.existsByUmcProductMemberIdAndRolesOnDate(
            member.getId(),
            managementRoles,
            LocalDate.of(2026, 8, 31)
        )).isTrue();
        assertThat(leadershipAdapter.existsByUmcProductMemberIdAndRolesOnDate(
            member.getId(),
            managementRoles,
            LocalDate.of(2026, 9, 1)
        )).isFalse();
    }

    @Test
    void 동일한_Leadership_역할은_같은_날에_한_명만_가질_수_있다() {
        UmcProductMember first = saveMember(106L);
        UmcProductMember second = saveMember(107L);
        UmcProductMemberActivityPeriod firstPeriod = saveActivityPeriod(first, START_DATE, END_DATE);
        UmcProductMemberActivityPeriod secondPeriod = saveActivityPeriod(second, START_DATE, END_DATE);
        leadershipAdapter.save(UmcProductLeadership.create(
            firstPeriod,
            UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD,
            START_DATE,
            LocalDate.of(2026, 6, 30)
        ));
        em.flush();

        assertThat(leadershipAdapter.existsOverlappingRole(
            UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD,
            LocalDate.of(2026, 6, 30),
            END_DATE,
            null
        )).isTrue();
        assertThatThrownBy(() -> {
            leadershipAdapter.save(UmcProductLeadership.create(
                secondPeriod,
                UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD,
                LocalDate.of(2026, 6, 30),
                END_DATE
            ));
            em.flush();
        }).isInstanceOf(InhouseDomainException.class)
            .satisfies(exception -> assertThat(((InhouseDomainException) exception).getBaseCode())
                .isEqualTo(InhouseErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED));
    }

    @Test
    void 한_멤버는_Lead와_Vice_Lead를_같은_날에_겸할_수_없다() {
        UmcProductMember member = saveMember(113L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, END_DATE);
        leadershipAdapter.save(UmcProductLeadership.create(
            period,
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
            START_DATE,
            LocalDate.of(2026, 6, 30)
        ));

        assertThatThrownBy(() -> leadershipAdapter.save(UmcProductLeadership.create(
            period,
            UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD,
            LocalDate.of(2026, 6, 30),
            END_DATE
        ))).isInstanceOf(InhouseDomainException.class)
            .satisfies(exception -> assertThat(((InhouseDomainException) exception).getBaseCode())
                .isEqualTo(InhouseErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED));
    }

    @Test
    void Department는_activeOn의_종료일을_포함하고_기간_밖의_Department를_제외한다() {
        UmcProductDepartment current = saveDepartment(
            "CURRENT",
            START_DATE,
            LocalDate.of(2026, 6, 30),
            2,
            true
        );
        saveDepartment("FUTURE", LocalDate.of(2026, 7, 1), null, 1, true);
        saveDepartment("INACTIVE", START_DATE, null, 3, false);
        em.flush();

        assertThat(departmentAdapter.listAll(true, LocalDate.of(2026, 6, 30)))
            .extracting(UmcProductDepartment::getId)
            .containsExactly(current.getId());
    }

    @Test
    void Department_코드_중복은_도메인_충돌로_변환한다() {
        saveDepartment("DUPLICATED", START_DATE, END_DATE, 1, true);

        assertThatThrownBy(() -> saveDepartment("DUPLICATED", START_DATE, END_DATE, 2, true))
            .isInstanceOf(InhouseDomainException.class)
            .satisfies(exception -> assertThat(((InhouseDomainException) exception).getBaseCode())
                .isEqualTo(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_ALREADY_EXISTS));
    }

    @Test
    void 같은_Department의_멤버_참여와_DEPARTMENT_LEAD_기간_중복을_조회한다() {
        UmcProductDepartment department = saveDepartment("RECRUIT", START_DATE, END_DATE, 1, true);
        UmcProductMember member = saveMember(108L);
        UmcProductMember secondMember = saveMember(112L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, END_DATE);
        UmcProductMemberActivityPeriod secondPeriod = saveActivityPeriod(
            secondMember, START_DATE, END_DATE
        );
        departmentParticipantAdapter.save(UmcProductDepartmentParticipant.create(
            department,
            period,
            UmcProductDepartmentRole.DEPARTMENT_LEAD,
            UmcProductPosition.PRODUCT_OWNER,
            "모집 정책",
            null,
            START_DATE,
            LocalDate.of(2026, 6, 30)
        ));
        em.flush();

        assertThat(departmentParticipantAdapter.existsOverlappingMemberInDepartment(
            department.getId(),
            member.getId(),
            LocalDate.of(2026, 6, 30),
            END_DATE,
            null
        )).isTrue();
        assertThat(departmentParticipantAdapter.existsOverlappingDepartmentLead(
            department.getId(),
            LocalDate.of(2026, 6, 30),
            END_DATE,
            null
        )).isTrue();
        assertThat(departmentParticipantAdapter.listByUmcProductMemberId(member.getId()))
            .singleElement()
            .satisfies(participant -> {
                assertThat(participant.getStartDate()).isEqualTo(START_DATE);
                assertThat(participant.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
            });
        assertThatThrownBy(() -> departmentParticipantAdapter.save(UmcProductDepartmentParticipant.create(
            department,
            secondPeriod,
            UmcProductDepartmentRole.DEPARTMENT_LEAD,
            UmcProductPosition.PRODUCT_DESIGNER,
            "후임 모집 리드",
            null,
            LocalDate.of(2026, 6, 30),
            END_DATE
        ))).isInstanceOf(InhouseDomainException.class)
            .satisfies(exception -> assertThat(((InhouseDomainException) exception).getBaseCode())
                .isEqualTo(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_LEAD_OVERLAPPED));
    }

    @Test
    void 멤버의_하위_활동을_FK_안전_순서로_일괄_삭제할_수_있다() {
        UmcProductChapter chapter = saveChapter("DELETE_CHAPTER", 1, true);
        UmcProductDepartment department = saveDepartment("DELETE_DEPARTMENT", START_DATE, END_DATE, 1, true);
        UmcProductMember member = saveMember(111L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, END_DATE);
        chapterMembershipAdapter.save(UmcProductChapterMembership.create(
            period,
            chapter,
            UmcProductPosition.SERVER_DEVELOPER,
            "삭제 대상",
            null,
            START_DATE,
            END_DATE
        ));
        leadershipAdapter.save(UmcProductLeadership.create(
            period,
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
            START_DATE,
            END_DATE
        ));
        departmentParticipantAdapter.save(UmcProductDepartmentParticipant.create(
            department,
            period,
            UmcProductDepartmentRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "삭제 대상",
            null,
            START_DATE,
            END_DATE
        ));
        em.flush();

        departmentParticipantAdapter.deleteAllByUmcProductMemberId(member.getId());
        chapterMembershipAdapter.deleteAllByUmcProductMemberId(member.getId());
        leadershipAdapter.deleteAllByUmcProductMemberId(member.getId());
        activityPeriodAdapter.deleteAllByUmcProductMemberId(member.getId());
        memberAdapter.delete(member);
        em.flush();
        em.clear();

        assertThat(memberAdapter.findById(member.getId())).isEmpty();
    }

    private UmcProductMember saveMember(Long memberId) {
        return memberAdapter.save(UmcProductMember.create(
            "테스트 " + memberId,
            "테스터" + memberId,
            null,
            "소개",
            null
        ));
    }

    private Member saveLoginAccount(String seed) {
        return em.persistAndFlush(Member.create(
            seed,
            seed,
            seed + "@test.com",
            null,
            null
        ));
    }

    private UmcProductMemberActivityPeriod saveActivityPeriod(
        UmcProductMember member,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return activityPeriodAdapter.save(UmcProductMemberActivityPeriod.create(member, startDate, endDate));
    }

    private UmcProductChapter saveChapter(String code, int sortOrder, boolean active) {
        return chapterAdapter.save(UmcProductChapter.create(
            code,
            code + " Chapter",
            null,
            sortOrder,
            active
        ));
    }

    private UmcProductDepartment saveDepartment(
        String code,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder,
        boolean active
    ) {
        return departmentAdapter.save(UmcProductDepartment.create(
            code,
            code + " Department",
            null,
            null,
            startDate,
            endDate,
            sortOrder,
            active
        ));
    }
}
