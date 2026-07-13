package com.umc.product.organization.adapter.out.persistence.umcproduct;

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

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductPart;
import com.umc.product.organization.domain.UmcProductPartMembership;
import com.umc.product.organization.domain.UmcProductSquad;
import com.umc.product.organization.domain.UmcProductSquadParticipant;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.LockModeType;

@PersistenceAdapterTest
@ActiveProfiles("test")
@Import({
    UmcProductMemberQueryRepository.class,
    UmcProductChapterPersistenceAdapter.class,
    UmcProductPartPersistenceAdapter.class,
    UmcProductMemberPersistenceAdapter.class,
    UmcProductMemberActivityPeriodPersistenceAdapter.class,
    UmcProductPartMembershipPersistenceAdapter.class,
    UmcProductLeadershipPersistenceAdapter.class,
    UmcProductSquadPersistenceAdapter.class,
    UmcProductSquadParticipantPersistenceAdapter.class
})
class UmcProductPersistenceAdapterTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Autowired
    TestEntityManager em;

    @Autowired
    UmcProductChapterPersistenceAdapter chapterAdapter;

    @Autowired
    UmcProductPartPersistenceAdapter partAdapter;

    @Autowired
    UmcProductMemberPersistenceAdapter memberAdapter;

    @Autowired
    UmcProductMemberActivityPeriodPersistenceAdapter activityPeriodAdapter;

    @Autowired
    UmcProductPartMembershipPersistenceAdapter partMembershipAdapter;

    @Autowired
    UmcProductLeadershipPersistenceAdapter leadershipAdapter;

    @Autowired
    UmcProductSquadPersistenceAdapter squadAdapter;

    @Autowired
    UmcProductSquadParticipantPersistenceAdapter squadParticipantAdapter;

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
    void Member_중복_DB_제약은_도메인_충돌로_변환한다() {
        saveMember(120L);

        assertThatThrownBy(() -> saveMember(120L))
            .isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_MEMBER_ALREADY_EXISTS));
    }

    @Test
    void Chapter_코드_중복_DB_제약은_도메인_충돌로_변환한다() {
        saveChapter("DUPLICATED_CHAPTER", 1, true);

        assertThatThrownBy(() -> saveChapter("DUPLICATED_CHAPTER", 2, true))
            .isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_ALREADY_EXISTS));
    }

    @Test
    void 같은_Chapter의_Part_코드_중복_DB_제약은_도메인_충돌로_변환한다() {
        UmcProductChapter chapter = saveChapter("PART_DUPLICATION", 1, true);
        savePart(chapter, "SERVER", 1, true);

        assertThatThrownBy(() -> savePart(chapter, "SERVER", 2, true))
            .isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_PART_ALREADY_EXISTS));
    }

    @Test
    void Chapter와_Part를_비관적_쓰기_잠금으로_조회한다() {
        UmcProductChapter chapter = saveChapter("LOCKED_CHAPTER", 1, true);
        UmcProductPart part = savePart(chapter, "LOCKED_PART", 1, true);
        em.flush();
        em.clear();

        UmcProductChapter lockedChapter = chapterAdapter.getByIdWithLock(chapter.getId());
        UmcProductPart lockedPart = partAdapter.getByIdWithLock(part.getId());

        assertThat(em.getEntityManager().getLockMode(lockedChapter))
            .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(em.getEntityManager().getLockMode(lockedPart))
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
        }).isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED));
    }

    @Test
    void Chapter와_Part는_별도_활성_필터와_Chapter_관계를_유지한다() {
        UmcProductChapter activeChapter = saveChapter("DEVELOP", 2, true);
        UmcProductChapter inactiveChapter = saveChapter("DESIGN", 1, false);
        UmcProductPart server = savePart(activeChapter, "SERVER", 2, true);
        UmcProductPart web = savePart(activeChapter, "WEB", 1, false);
        savePart(inactiveChapter, "PRODUCT_DESIGN", 1, true);
        em.flush();
        em.clear();

        assertThat(chapterAdapter.listAll(true))
            .extracting(UmcProductChapter::getCode)
            .containsExactly("DEVELOP");
        assertThat(partAdapter.listAll(activeChapter.getId(), null))
            .extracting(UmcProductPart::getCode)
            .containsExactly("WEB", "SERVER");
        assertThat(partAdapter.listAll(activeChapter.getId(), true))
            .singleElement()
            .satisfies(part -> {
                assertThat(part.getId()).isEqualTo(server.getId());
                assertThat(part.getChapter().getId()).isEqualTo(activeChapter.getId());
            });
        assertThat(partAdapter.existsByChapterIdAndCode(
            activeChapter.getId(),
            web.getCode(),
            null
        )).isTrue();
    }

    @Test
    void 멤버는_같은_기간에_여러_Chapter의_Part에_소속될_수_있다() {
        UmcProductChapter developChapter = saveChapter("DEVELOP", 1, true);
        UmcProductChapter clientChapter = saveChapter("CLIENT", 2, true);
        UmcProductPart server = savePart(developChapter, "SERVER", 1, true);
        UmcProductPart web = savePart(clientChapter, "WEB", 2, true);
        UmcProductMember member = saveMember(102L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, null);

        partMembershipAdapter.save(UmcProductPartMembership.create(
            period,
            server,
            UmcProductPartRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "API 개발",
            null,
            START_DATE,
            null
        ));
        partMembershipAdapter.save(UmcProductPartMembership.create(
            period,
            web,
            UmcProductPartRole.MEMBER,
            UmcProductPosition.WEB_DEVELOPER,
            "웹 개발",
            null,
            START_DATE,
            null
        ));
        em.flush();
        em.clear();

        assertThat(partMembershipAdapter.listByUmcProductMemberId(member.getId()))
            .extracting(membership -> membership.getPart().getCode())
            .containsExactlyInAnyOrder("SERVER", "WEB");
    }

    @Test
    void 멤버_검색의_activeOn은_멤버와_Part_소속_기간을_모두_검사한다() {
        UmcProductChapter chapter = saveChapter("ENGINEERING", 1, true);
        UmcProductPart part = savePart(chapter, "SERVER", 1, true);
        UmcProductMember currentMember = saveMember(109L);
        UmcProductMember pastMember = saveMember(110L);
        UmcProductMemberActivityPeriod currentPeriod = saveActivityPeriod(currentMember, START_DATE, null);
        UmcProductMemberActivityPeriod pastPeriod = saveActivityPeriod(
            pastMember,
            START_DATE,
            LocalDate.of(2026, 3, 31)
        );
        partMembershipAdapter.save(UmcProductPartMembership.create(
            currentPeriod,
            part,
            UmcProductPartRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "API 개발",
            null,
            START_DATE,
            null
        ));
        partMembershipAdapter.save(UmcProductPartMembership.create(
            pastPeriod,
            part,
            UmcProductPartRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "API 개발",
            null,
            START_DATE,
            LocalDate.of(2026, 3, 31)
        ));
        em.flush();

        UmcProductMemberSearchCondition condition = UmcProductMemberSearchCondition.of(
            chapter.getId(),
            part.getId(),
            UmcProductPartRole.MEMBER,
            null,
            UmcProductPosition.SERVER_DEVELOPER,
            null,
            LocalDate.of(2026, 4, 1)
        );

        assertThat(memberAdapter.searchIds(condition, PageRequest.of(0, 10)).getContent())
            .containsExactly(currentMember.getId());
    }

    @Test
    void 같은_Part의_PART_LEAD_기간은_겹칠_수_없다() {
        UmcProductChapter chapter = saveChapter("PRODUCT", 1, true);
        UmcProductPart part = savePart(chapter, "OPERATION", 1, true);
        UmcProductMember first = saveMember(103L);
        UmcProductMember second = saveMember(104L);
        UmcProductMemberActivityPeriod firstPeriod = saveActivityPeriod(first, START_DATE, END_DATE);
        UmcProductMemberActivityPeriod secondPeriod = saveActivityPeriod(second, START_DATE, END_DATE);

        partMembershipAdapter.save(UmcProductPartMembership.create(
            firstPeriod,
            part,
            UmcProductPartRole.PART_LEAD,
            UmcProductPosition.PRODUCT_OWNER,
            "운영 리드",
            null,
            START_DATE,
            LocalDate.of(2026, 6, 30)
        ));
        em.flush();

        assertThat(partMembershipAdapter.existsOverlappingPartLead(
            part.getId(),
            LocalDate.of(2026, 6, 30),
            END_DATE,
            null
        )).isTrue();
        assertThatThrownBy(() -> {
            partMembershipAdapter.save(UmcProductPartMembership.create(
                secondPeriod,
                part,
                UmcProductPartRole.PART_LEAD,
                UmcProductPosition.PRODUCT_OWNER,
                "후임 운영 리드",
                null,
                LocalDate.of(2026, 6, 30),
                END_DATE
            ));
            em.flush();
        }).isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_PART_LEAD_OVERLAPPED));
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

        assertThat(leadershipAdapter.existsByMemberIdAndRolesOnDate(
            member.getMemberId(),
            managementRoles,
            LocalDate.of(2026, 8, 31)
        )).isTrue();
        assertThat(leadershipAdapter.existsByMemberIdAndRolesOnDate(
            member.getMemberId(),
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
        }).isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED));
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
        ))).isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED));
    }

    @Test
    void Squad는_activeOn의_종료일을_포함하고_기간_밖의_Squad를_제외한다() {
        UmcProductSquad current = saveSquad(
            "CURRENT",
            START_DATE,
            LocalDate.of(2026, 6, 30),
            2,
            true
        );
        saveSquad("FUTURE", LocalDate.of(2026, 7, 1), null, 1, true);
        saveSquad("INACTIVE", START_DATE, null, 3, false);
        em.flush();

        assertThat(squadAdapter.listAll(true, LocalDate.of(2026, 6, 30)))
            .extracting(UmcProductSquad::getId)
            .containsExactly(current.getId());
    }

    @Test
    void Squad_코드_중복은_도메인_충돌로_변환한다() {
        saveSquad("DUPLICATED", START_DATE, END_DATE, 1, true);

        assertThatThrownBy(() -> saveSquad("DUPLICATED", START_DATE, END_DATE, 2, true))
            .isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_SQUAD_ALREADY_EXISTS));
    }

    @Test
    void 같은_Squad의_멤버_참여와_SQUAD_LEAD_기간_중복을_조회한다() {
        UmcProductSquad squad = saveSquad("RECRUIT", START_DATE, END_DATE, 1, true);
        UmcProductMember member = saveMember(108L);
        UmcProductMember secondMember = saveMember(112L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, END_DATE);
        UmcProductMemberActivityPeriod secondPeriod = saveActivityPeriod(
            secondMember, START_DATE, END_DATE
        );
        squadParticipantAdapter.save(UmcProductSquadParticipant.create(
            squad,
            period,
            UmcProductSquadRole.SQUAD_LEAD,
            UmcProductPosition.PRODUCT_OWNER,
            "모집 정책",
            null,
            START_DATE,
            LocalDate.of(2026, 6, 30)
        ));
        em.flush();

        assertThat(squadParticipantAdapter.existsOverlappingMemberInSquad(
            squad.getId(),
            member.getId(),
            LocalDate.of(2026, 6, 30),
            END_DATE,
            null
        )).isTrue();
        assertThat(squadParticipantAdapter.existsOverlappingSquadLead(
            squad.getId(),
            LocalDate.of(2026, 6, 30),
            END_DATE,
            null
        )).isTrue();
        assertThat(squadParticipantAdapter.listByUmcProductMemberId(member.getId()))
            .singleElement()
            .satisfies(participant -> {
                assertThat(participant.getStartDate()).isEqualTo(START_DATE);
                assertThat(participant.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
            });
        assertThatThrownBy(() -> squadParticipantAdapter.save(UmcProductSquadParticipant.create(
            squad,
            secondPeriod,
            UmcProductSquadRole.SQUAD_LEAD,
            UmcProductPosition.PRODUCT_DESIGNER,
            "후임 모집 리드",
            null,
            LocalDate.of(2026, 6, 30),
            END_DATE
        ))).isInstanceOf(OrganizationDomainException.class)
            .satisfies(exception -> assertThat(((OrganizationDomainException) exception).getBaseCode())
                .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_SQUAD_LEAD_OVERLAPPED));
    }

    @Test
    void 멤버의_하위_활동을_FK_안전_순서로_일괄_삭제할_수_있다() {
        UmcProductChapter chapter = saveChapter("DELETE_CHAPTER", 1, true);
        UmcProductPart part = savePart(chapter, "DELETE_PART", 1, true);
        UmcProductSquad squad = saveSquad("DELETE_SQUAD", START_DATE, END_DATE, 1, true);
        UmcProductMember member = saveMember(111L);
        UmcProductMemberActivityPeriod period = saveActivityPeriod(member, START_DATE, END_DATE);
        partMembershipAdapter.save(UmcProductPartMembership.create(
            period,
            part,
            UmcProductPartRole.MEMBER,
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
        squadParticipantAdapter.save(UmcProductSquadParticipant.create(
            squad,
            period,
            UmcProductSquadRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "삭제 대상",
            null,
            START_DATE,
            END_DATE
        ));
        em.flush();

        squadParticipantAdapter.deleteAllByUmcProductMemberId(member.getId());
        partMembershipAdapter.deleteAllByUmcProductMemberId(member.getId());
        leadershipAdapter.deleteAllByUmcProductMemberId(member.getId());
        activityPeriodAdapter.deleteAllByUmcProductMemberId(member.getId());
        memberAdapter.delete(member);
        em.flush();
        em.clear();

        assertThat(memberAdapter.findById(member.getId())).isEmpty();
    }

    private UmcProductMember saveMember(Long memberId) {
        return memberAdapter.save(UmcProductMember.create(memberId, "소개", null));
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

    private UmcProductPart savePart(
        UmcProductChapter chapter,
        String code,
        int sortOrder,
        boolean active
    ) {
        return partAdapter.save(UmcProductPart.create(
            chapter,
            code,
            code + " Part",
            null,
            sortOrder,
            active
        ));
    }

    private UmcProductSquad saveSquad(
        String code,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder,
        boolean active
    ) {
        return squadAdapter.save(UmcProductSquad.create(
            code,
            code + " Squad",
            null,
            startDate,
            endDate,
            sortOrder,
            active
        ));
    }
}
