package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.application.port.out.query.dto.UmcProductMemberSearchCriteria;
import com.umc.product.inhouse.domain.UmcProductChapterMembership;
import com.umc.product.inhouse.domain.UmcProductDepartment;
import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.UmcProductLeadership;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductMemberQueryService implements GetUmcProductMemberUseCase {

    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final LoadUmcProductMemberAccountPort loadUmcProductMemberAccountPort;
    private final LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    private final LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;
    private final LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    private final LoadUmcProductDepartmentParticipantPort loadUmcProductDepartmentParticipantPort;
    private final LoadUmcProductDepartmentPort loadUmcProductDepartmentPort;
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetFileUseCase getFileUseCase;

    @Override
    public UmcProductMemberInfo getById(Long umcProductMemberId) {
        UmcProductMember member = loadUmcProductMemberPort.getById(umcProductMemberId);
        List<UmcProductDepartmentParticipant> departmentParticipations = loadUmcProductDepartmentParticipantPort
            .listByUmcProductMemberId(umcProductMemberId);
        Map<Long, String> schoolNames = resolveSchoolNames(List.of(member));
        Map<String, String> productProfileLinks = resolveProductProfileLinks(List.of(member));
        return toInfo(
            member,
            schoolNames.get(member.getSchoolId()),
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
    public Optional<UmcProductMemberInfo> findByAccountMemberId(Long memberId) {
        return loadUmcProductMemberAccountPort.findByMemberId(memberId)
            .map(account -> getById(account.getUmcProductMember().getId()));
    }

    @Override
    public Page<UmcProductMemberInfo> search(UmcProductMemberSearchCondition condition, Pageable pageable) {
        UmcProductMemberSearchCriteria criteria = toSearchCriteria(condition);
        Page<Long> idPage = loadUmcProductMemberPort.searchIds(criteria, pageable);
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
        Map<Long, String> schoolNames = resolveSchoolNames(memberMap.values());
        Map<String, String> productProfileLinks = resolveProductProfileLinks(memberMap.values());

        List<UmcProductMemberInfo> content = ids.stream()
            .map(memberMap::get)
            .filter(Objects::nonNull)
            .map(member -> toInfo(
                member,
                schoolNames.get(member.getSchoolId()),
                periodsByMember.getOrDefault(member.getId(), List.of()),
                membershipsByMember.getOrDefault(member.getId(), List.of()),
                leadershipsByMember.getOrDefault(member.getId(), List.of()),
                departmentsByMember.getOrDefault(member.getId(), List.of()),
                departmentMap,
                productProfileLinkOf(productProfileLinks, member),
                criteria.activeOn()
            ))
            .toList();
        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    private UmcProductMemberSearchCriteria toSearchCriteria(UmcProductMemberSearchCondition condition) {
        if (condition == null) {
            return new UmcProductMemberSearchCriteria(null, null, null, null, null);
        }
        return new UmcProductMemberSearchCriteria(
            condition.chapterId(),
            condition.leadershipRole(),
            condition.position(),
            resolveDepartmentIds(condition.departmentId(), condition.includeDescendants()),
            condition.activeOn()
        );
    }

    private Set<Long> resolveDepartmentIds(Long departmentId, boolean includeDescendants) {
        if (departmentId == null) {
            return null;
        }
        if (!includeDescendants) {
            return Set.of(departmentId);
        }

        Map<Long, List<Long>> childIdsByParent = loadUmcProductDepartmentPort.listAll(null, null).stream()
            .filter(department -> department.getParent() != null)
            .collect(Collectors.groupingBy(
                department -> department.getParent().getId(),
                Collectors.mapping(UmcProductDepartment::getId, Collectors.toList())
            ));
        Set<Long> departmentIds = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(departmentId);
        while (!queue.isEmpty()) {
            Long currentId = queue.removeFirst();
            if (departmentIds.add(currentId)) {
                queue.addAll(childIdsByParent.getOrDefault(currentId, List.of()));
            }
        }
        return departmentIds;
    }

    private UmcProductMemberInfo toInfo(
        UmcProductMember member,
        String schoolName,
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
            member.getName(),
            member.getNickname(),
            member.getSchoolId(),
            schoolName,
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

    private Map<Long, String> resolveSchoolNames(Collection<UmcProductMember> members) {
        Set<Long> schoolIds = members.stream()
            .map(UmcProductMember::getSchoolId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (schoolIds.isEmpty()) {
            return Map.of();
        }
        return getSchoolUseCase.listDetailsByIds(schoolIds).stream()
            .collect(Collectors.toMap(SchoolDetailInfo::schoolId, SchoolDetailInfo::schoolName));
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
