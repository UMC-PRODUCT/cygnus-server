package com.umc.product.schedule.adapter.in.graphql;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.schedule.adapter.in.graphql.dto.ScheduleGraphQlResponse;
import com.umc.product.schedule.application.port.in.query.GetScheduleCapabilitiesUseCase;
import com.umc.product.schedule.application.port.in.query.GetScheduleUseCase;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleCapabilitiesInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScheduleGraphQlController {

    private final GetScheduleUseCase getScheduleUseCase;
    private final GetScheduleCapabilitiesUseCase getScheduleCapabilitiesUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @QueryMapping
    public List<ScheduleGraphQlResponse> mySchedules(
        @CurrentMember MemberPrincipal principal,
        @Argument ScheduleFilter filter
    ) {
        return getScheduleUseCase.searchMySchedules(
            filter.from(),
            filter.to(),
            filter.attendanceRequired(),
            principal.getMemberId()
        ).stream().map(ScheduleGraphQlResponse::from).toList();
    }

    @QueryMapping
    public ScheduleGraphQlResponse schedule(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        return ScheduleGraphQlResponse.from(
            getScheduleUseCase.getScheduleDetails(id, principal.getMemberId())
        );
    }

    @QueryMapping
    public ScheduleCapabilitiesInfo myScheduleCapabilities(@CurrentMember MemberPrincipal principal) {
        return getScheduleCapabilitiesUseCase.getCapabilities(principal.getMemberId());
    }

    @BatchMapping(typeName = "Schedule", field = "author")
    public Map<ScheduleGraphQlResponse, MemberPublicInfo> authors(List<ScheduleGraphQlResponse> schedules) {
        Map<ScheduleGraphQlResponse, List<MemberPublicInfo>> members = memberMap(
            schedules,
            schedule -> List.of(schedule.authorMemberId())
        );
        Map<ScheduleGraphQlResponse, MemberPublicInfo> result = new LinkedHashMap<>();
        members.forEach((schedule, values) -> result.put(schedule, values.getFirst()));
        return result;
    }

    @BatchMapping(typeName = "Schedule", field = "participants")
    public Map<ScheduleGraphQlResponse, List<MemberPublicInfo>> participants(
        List<ScheduleGraphQlResponse> schedules
    ) {
        return memberMap(schedules, ScheduleGraphQlResponse::participantMemberIds);
    }

    private <T> Map<T, List<MemberPublicInfo>> memberMap(
        List<T> sources,
        java.util.function.Function<T, List<Long>> memberIdsExtractor
    ) {
        Set<Long> memberIds = sources.stream()
            .flatMap(source -> memberIdsExtractor.apply(source).stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<T, List<MemberPublicInfo>> result = new LinkedHashMap<>();
        for (T source : sources) {
            result.put(
                source,
                memberIdsExtractor.apply(source).stream()
                    .map(membersById::get)
                    .filter(java.util.Objects::nonNull)
                    .toList()
            );
        }
        return result;
    }

    public record ScheduleFilter(
        Instant from,
        Instant to,
        Boolean attendanceRequired
    ) {
    }
}
