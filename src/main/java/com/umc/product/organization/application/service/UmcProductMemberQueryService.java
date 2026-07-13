package com.umc.product.organization.application.service;

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

import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetUmcProductMemberUseCase;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductLeadershipInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberActivityPeriodInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartMembershipInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadParticipationInfo;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductPartMembership;
import com.umc.product.organization.domain.UmcProductSquadParticipant;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductMemberQueryService implements GetUmcProductMemberUseCase {

    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    private final LoadUmcProductPartMembershipPort loadUmcProductPartMembershipPort;
    private final LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    private final LoadUmcProductSquadParticipantPort loadUmcProductSquadParticipantPort;
    private final LoadUmcProductSquadPort loadUmcProductSquadPort;
    private final GetMemberUseCase getMemberUseCase;
    private final GetFileUseCase getFileUseCase;

    @Override
    public UmcProductMemberInfo getById(Long umcProductMemberId) {
        UmcProductMember member = loadUmcProductMemberPort.getById(umcProductMemberId);
        MemberInfo memberInfo = getMemberUseCase.findById(member.getMemberId()).orElse(null);
        List<UmcProductSquadParticipant> squadParticipations = loadUmcProductSquadParticipantPort
            .listByUmcProductMemberId(umcProductMemberId);
        Map<String, String> productProfileLinks = resolveProductProfileLinks(List.of(member));
        return toInfo(
            member,
            memberInfo,
            loadUmcProductMemberActivityPeriodPort.listByUmcProductMemberId(umcProductMemberId),
            loadUmcProductPartMembershipPort.listByUmcProductMemberId(umcProductMemberId),
            loadUmcProductLeadershipPort.listByUmcProductMemberId(umcProductMemberId),
            squadParticipations,
            squadMapOf(squadParticipations),
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
        List<UmcProductPartMembership> memberships = loadUmcProductPartMembershipPort
            .listByUmcProductMemberIds(ids);
        List<UmcProductLeadership> leaderships = loadUmcProductLeadershipPort.listByUmcProductMemberIds(ids);
        List<UmcProductSquadParticipant> squadParticipations = loadUmcProductSquadParticipantPort
            .listByUmcProductMemberIds(ids);

        Map<Long, List<UmcProductMemberActivityPeriod>> periodsByMember = periods.stream()
            .collect(Collectors.groupingBy(period -> period.getUmcProductMember().getId()));
        Map<Long, List<UmcProductPartMembership>> membershipsByMember = memberships.stream()
            .collect(Collectors.groupingBy(item -> item.getMemberActivityPeriod().getUmcProductMember().getId()));
        Map<Long, List<UmcProductLeadership>> leadershipsByMember = leaderships.stream()
            .collect(Collectors.groupingBy(item -> item.getMemberActivityPeriod().getUmcProductMember().getId()));
        Map<Long, List<UmcProductSquadParticipant>> squadsByMember = squadParticipations.stream()
            .collect(Collectors.groupingBy(item -> item.getMemberActivityPeriod().getUmcProductMember().getId()));
        Map<Long, UmcProductSquadInfo> squadMap = squadMapOf(squadParticipations);
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
                squadsByMember.getOrDefault(member.getId(), List.of()),
                squadMap,
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
        List<UmcProductPartMembership> memberships,
        List<UmcProductLeadership> leaderships,
        List<UmcProductSquadParticipant> squadParticipations,
        Map<Long, UmcProductSquadInfo> squadMap,
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
                .map(item -> UmcProductPartMembershipInfo.from(item, UmcProductPartInfo.from(item.getPart())))
                .toList(),
            leaderships.stream()
                .filter(item -> activeOn == null || item.isActiveOn(activeOn))
                .sorted(historyComparator())
                .map(UmcProductLeadershipInfo::from)
                .toList(),
            squadParticipations.stream()
                .filter(item -> activeOn == null || item.isActiveOn(activeOn))
                .sorted(historyComparator())
                .map(item -> UmcProductSquadParticipationInfo.from(item, squadMap.get(item.getSquad().getId())))
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
        if (value instanceof UmcProductPartMembership membership) {
            return membership.getStartDate();
        }
        if (value instanceof UmcProductLeadership leadership) {
            return leadership.getStartDate();
        }
        return ((UmcProductSquadParticipant) value).getStartDate();
    }

    private Long idOf(Object value) {
        if (value instanceof UmcProductMemberActivityPeriod period) {
            return period.getId();
        }
        if (value instanceof UmcProductPartMembership membership) {
            return membership.getId();
        }
        if (value instanceof UmcProductLeadership leadership) {
            return leadership.getId();
        }
        return ((UmcProductSquadParticipant) value).getId();
    }

    private Map<Long, UmcProductSquadInfo> squadMapOf(List<UmcProductSquadParticipant> participations) {
        Set<Long> squadIds = participations.stream()
            .map(participation -> participation.getSquad().getId())
            .collect(Collectors.toSet());
        if (squadIds.isEmpty()) {
            return Map.of();
        }
        return loadUmcProductSquadPort.listByIds(squadIds).stream()
            .map(UmcProductSquadInfo::from)
            .collect(Collectors.toMap(UmcProductSquadInfo::squadId, Function.identity()));
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
