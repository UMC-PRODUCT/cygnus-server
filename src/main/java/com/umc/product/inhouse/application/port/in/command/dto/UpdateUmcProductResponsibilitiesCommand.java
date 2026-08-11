package com.umc.product.inhouse.application.port.in.command.dto;

import java.util.List;

public record UpdateUmcProductResponsibilitiesCommand(
    Long requesterMemberId,
    Long umcProductMemberId,
    List<ChapterResponsibility> chapterMemberships,
    List<DepartmentResponsibility> departmentParticipations
) {
    public UpdateUmcProductResponsibilitiesCommand {
        chapterMemberships = immutable(chapterMemberships);
        departmentParticipations = immutable(departmentParticipations);
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record ChapterResponsibility(
        Long chapterMembershipId,
        String responsibilityTitle,
        String responsibilityDescription
    ) {
    }

    public record DepartmentResponsibility(
        Long departmentParticipantId,
        String responsibilityTitle,
        String responsibilityDescription
    ) {
    }
}
