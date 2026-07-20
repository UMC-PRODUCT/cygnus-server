package com.umc.product.support.fixture;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductChapterMembership;
import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductSquad;
import com.umc.product.organization.domain.UmcProductSquadParticipant;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;

public final class OrganizationUnitFixture {

    private OrganizationUnitFixture() {
    }

    public static Gisu 기수(Long id, Long generation, boolean active) {
        Gisu gisu = Gisu.create(
            generation,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-12-31T23:59:59Z"),
            active
        );
        setId(gisu, id);
        return gisu;
    }

    public static Chapter 지부(Long id, Gisu gisu, String name) {
        Chapter chapter = Chapter.create(gisu, name);
        setId(chapter, id);
        return chapter;
    }

    public static School 학교(Long id, String name) {
        School school = School.create(name, "비고");
        setId(school, id);
        return school;
    }

    public static ChapterSchool 지부_학교(Long id, Chapter chapter, School school) {
        ChapterSchool chapterSchool = ChapterSchool.create(chapter, school);
        setId(chapterSchool, id);
        return chapterSchool;
    }

    public static StudyGroup 스터디_그룹(
        Long id,
        Long gisuId,
        Set<Long> mentorIds,
        Set<Long> memberIds
    ) {
        StudyGroup group = StudyGroup.create(
            "스프링 스터디", gisuId, ChallengerPart.SPRINGBOOT, memberIds, mentorIds
        );
        setId(group, id);
        return group;
    }

    public static UmcProductMember 프로덕트_멤버(Long id, Long memberId, String profileImageId) {
        UmcProductMember member = UmcProductMember.create(memberId, "소개", profileImageId);
        setId(member, id);
        return member;
    }

    public static UmcProductMemberActivityPeriod 활동_기간(
        Long id,
        UmcProductMember member,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductMemberActivityPeriod period = UmcProductMemberActivityPeriod.create(
            member,
            startDate,
            endDate
        );
        setId(period, id);
        return period;
    }

    public static UmcProductChapter 프로덕트_챕터(Long id) {
        UmcProductChapter chapter = UmcProductChapter.create("SERVER", "Server", "서버 챕터", 1, true);
        setId(chapter, id);
        return chapter;
    }

    public static UmcProductChapterMembership 챕터_소속(
        Long id,
        UmcProductMemberActivityPeriod period,
        UmcProductChapter chapter,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductChapterMembership membership = UmcProductChapterMembership.create(
            period,
            chapter,
            UmcProductPosition.SERVER_DEVELOPER,
            "Backend",
            "API 개발",
            startDate,
            endDate
        );
        setId(membership, id);
        return membership;
    }

    public static UmcProductLeadership 리더십(
        Long id,
        UmcProductMemberActivityPeriod period,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductLeadership leadership = UmcProductLeadership.create(
            period,
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
            startDate,
            endDate
        );
        setId(leadership, id);
        return leadership;
    }

    public static UmcProductSquad 스쿼드(Long id, LocalDate startDate, LocalDate endDate) {
        UmcProductSquad squad = UmcProductSquad.create(
            "PLATFORM",
            "Platform",
            "플랫폼 스쿼드",
            startDate,
            endDate,
            1,
            true
        );
        setId(squad, id);
        return squad;
    }

    public static UmcProductSquadParticipant 스쿼드_참여(
        Long id,
        UmcProductSquad squad,
        UmcProductMemberActivityPeriod period,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductSquadParticipant participant = UmcProductSquadParticipant.create(
            squad,
            period,
            UmcProductSquadRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "Backend",
            "API 개발",
            startDate,
            endDate
        );
        setId(participant, id);
        return participant;
    }

    private static void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
