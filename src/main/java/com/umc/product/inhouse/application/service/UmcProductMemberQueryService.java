package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.inhouse.application.port.in.query.GetUmcProductMemberUseCase;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterMembershipInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentParticipationInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductLeadershipInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberActivityPeriodInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberSearchCondition;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductChapterMembership;
import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.UmcProductLeadership;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductMemberQueryService implements GetUmcProductMemberUseCase {

    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    private final LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;
    private final LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    private final LoadUmcProductDepartmentParticipantPort loadUmcProductDepartmentParticipantPort;
    private final LoadUmcProductDepartmentPort loadUmcProductDepartmentPort;
    private final GetMemberUseCase getMemberUseCase;
    private final GetFileUseCase getFileUseCase;

    @Override
    public UmcProductMemberInfo getById(Long umcProductMemberId) {
        UmcProductMember member = loadUmcProductMemberPort.getById(umcProductMemberId);
        MemberInfo memberInfo = getMemberUseCase.findById(member.getMemberId()).orElse(null);
        List<UmcProductDepartmentParticipant> departmentParticipations = loadUmcProductDepartmentParticipantPort
            .listByUmcProductMemberId(umcProductMemberId);
        Map<String, String> productProfileLinks = resolveProductProfileLinks(List.of(member));
        return toInfo(
            member,
            memberInfo,
            loadUmcProductMemberActivityPeriodPort.listByUmcProductMemberId(umcProductMemberId),
            loadUmcProductChapterMembershipPort.listByUmcProductMemberId(umcProductMemberId),
            loadUmcProductLeadershipPort.listByUmcProductMemberId(umcProductMemberId),
            departmentParticipations,
            departmentMapOf(departmentParticipations),
            productProfileLinkOf(productProfileLinks, member),
            null
        );
    }

    @Override
    public Page<UmcProductMemberInfo> search(UmcProductMemberSearchCondition condition, Pageable pageable) {
        Page<Long> idPage = loadUmcProductMemberPort.searchIds(condition, pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> ids = idPage.getContent();
        Map<Long, UmcProductMember> memberMap = loadUmcProductMemberPort.listByIds(ids).stream()
            .collect(Collectors.toMap(UmcProductMember::getId, Function.identity()));
        List<UmcProductMemberActivityPeriod> periods = loadUmcProductMemberActivityPeriodPort
            .listByUmcProductMemberIds(ids);
        List<UmcProductChapterMembership> memberships = loadUmcProductChapterMembershipPort
            .listByUmcProductMemberIds(ids);
        List<UmcProductLeadership> leaderships = loadUmcProductLeadershipPort.listByUmcProductMemberIds(ids);
        List<UmcProductDepartmentParticipant> departmentParticipations = loadUmcProductDepartmentParticipantPort
            .listByUmcProductMemberIds(ids);

        Map<Long, List<UmcProductMemberActivityPeriod>> periodsByMember = periods.stream()
            .collect(Collectors.groupingBy(period -> period.getUmcProductMember().getId()));
        Map<Long, List<UmcProductChapterMembership>> membershipsByMember = memberships.stream()
            .collect(Collectors.groupingBy(item -> item.getMemberActivityPeriod().getUmcProductMember().getId()));
        Map<Long, List<UmcProductLeadership>> leadershipsByMember = leaderships.stream()
            .collect(Collectors.groupingBy(item -> item.getMemberActivityPeriod().getUmcProductMember().getId()));
        Map<Long, List<UmcProductDepartmentParticipant>> departmentsByMember = departmentParticipations.stream()
            .collect(Collectors.groupingBy(item -> item.getMemberActivityPeriod().getUmcProductMember().getId()));
        Map<Long, UmcProductDepartmentInfo> departmentMap = departmentMapOf(departmentParticipations);
        Map<Long, MemberInfo> memberInfoMap = resolveMemberInfos(memberMap.values());
        Map<String, String> productProfileLinks = resolveProductProfileLinks(memberMap.values());

        List<UmcProductMemberInfo> content = ids.stream()
            .map(memberMap::get)
            .filter(Objects::nonNull)
            .map(member -> toInfo(
                member,
                memberInfoMap.get(member.getMemberId()),
                periodsByMember.getOrDefault(member.getId(), List.of()),
                membershipsByMember.getOrDefault(member.getId(), List.of()),
                leadershipsByMember.getOrDefault(member.getId(), List.of()),
                departmentsByMember.getOrDefault(member.getId(), List.of()),
                departmentMap,
                productProfileLinkOf(productProfileLinks, member),
                condition.activeOn()
            ))
            .toList();
        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    private UmcProductMemberInfo toInfo(
        UmcProductMember member,
        MemberInfo memberInfo,
        List<UmcProductMemberActivityPeriod> periods,
        List<UmcProductChapterMembership> memberships,
        List<UmcProductLeadership> leaderships,
        List<UmcProductDepartmentParticipant> departmentParticipations,
        Map<Long, UmcProductDepartmentInfo> departmentMap,
        String umcProductProfileImageUrl,
        LocalDate activeOn
    ) {
        return new UmcProductMemberInfo(
            member.getId(),
            member.getMemberId(),
            memberInfo == null ? null : memberInfo.name(),
            memberInfo == null ? null : memberInfo.nickname(),
            memberInfo == null ? null : memberInfo.schoolName(),
            memberInfo == null ? null : memberInfo.profileImageId(),
            memberInfo == null ? null : memberInfo.profileImageLink(),
            member.getIntroduction(),
            member.getProfileImageId(),
            umcProductProfileImageUrl,
            periods.stream()
                .filter(item -> activeOn == null || item.isActiveOn(activeOn))
                .sorted(historyComparator())
                .map(UmcProductMemberActivityPeriodInfo::from)
                .toList(),
            memberships.stream()
                .filter(item -> activeOn == null || item.isActiveOn(activeOn))
                .sorted(historyComparator())
                .map(item -> UmcProductChapterMembershipInfo.from(
                    item,
                    UmcProductChapterInfo.from(item.getChapter())
                ))
                .toList(),
            leaderships.stream()
                .filter(item -> activeOn == null || item.isActiveOn(activeOn))
                .sorted(historyComparator())
                .map(UmcProductLeadershipInfo::from)
                .toList(),
            departmentParticipations.stream()
                .filter(item -> activeOn == null || item.isActiveOn(activeOn))
                .sorted(historyComparator())
                .map(item -> UmcProductDepartmentParticipationInfo.from(item, departmentMap.get(item.getDepartment().getId())))
                .toList()
        );
    }

    private <T> Comparator<T> historyComparator() {
        return Comparator
            .<T, LocalDate>comparing(
                value -> startDateOf(value),
                Comparator.nullsLast(Comparator.reverseOrder())
            )
            .thenComparing(
                value -> idOf(value),
                Comparator.nullsLast(Comparator.reverseOrder())
            );
    }

    private LocalDate startDateOf(Object value) {
        if (value instanceof UmcProductMemberActivityPeriod period) {
            return period.getStartDate();
        }
        if (value instanceof UmcProductChapterMembership membership) {
            return membership.getStartDate();
        }
        if (value instanceof UmcProductLeadership leadership) {
            return leadership.getStartDate();
        }
        return ((UmcProductDepartmentParticipant) value).getStartDate();
    }

    private Long idOf(Object value) {
        if (value instanceof UmcProductMemberActivityPeriod period) {
            return period.getId();
        }
        if (value instanceof UmcProductChapterMembership membership) {
            return membership.getId();
        }
        if (value instanceof UmcProductLeadership leadership) {
            return leadership.getId();
        }
        return ((UmcProductDepartmentParticipant) value).getId();
    }

    private Map<Long, UmcProductDepartmentInfo> departmentMapOf(List<UmcProductDepartmentParticipant> participations) {
        Set<Long> departmentIds = participations.stream()
            .map(participation -> participation.getDepartment().getId())
            .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Map.of();
        }
        return loadUmcProductDepartmentPort.listByIds(departmentIds).stream()
            .map(UmcProductDepartmentInfo::from)
            .collect(Collectors.toMap(UmcProductDepartmentInfo::departmentId, Function.identity()));
    }

    private Map<Long, MemberInfo> resolveMemberInfos(Collection<UmcProductMember> members) {
        Set<Long> memberIds = members.stream()
            .map(UmcProductMember::getMemberId)
            .collect(Collectors.toSet());
        return getMemberUseCase.findAllByIds(memberIds);
    }

    private Map<String, String> resolveProductProfileLinks(Collection<UmcProductMember> members) {
        List<String> imageIds = members.stream()
            .map(UmcProductMember::getProfileImageId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        return imageIds.isEmpty() ? Map.of() : getFileUseCase.getFileLinks(new ArrayList<>(imageIds));
    }

    private String productProfileLinkOf(Map<String, String> productProfileLinks, UmcProductMember member) {
        return member.getProfileImageId() == null ? null : productProfileLinks.get(member.getProfileImageId());
    }
}
