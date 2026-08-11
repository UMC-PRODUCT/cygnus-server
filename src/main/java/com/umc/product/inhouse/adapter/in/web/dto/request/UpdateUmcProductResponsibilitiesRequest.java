package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductResponsibilitiesCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUmcProductResponsibilitiesRequest(
    List<@NotNull @Valid ChapterResponsibility> chapterMemberships,
    List<@NotNull @Valid DepartmentResponsibility> departmentParticipations
) {
    public UpdateUmcProductResponsibilitiesRequest {
        chapterMemberships = immutable(chapterMemberships);
        departmentParticipations = immutable(departmentParticipations);
    }

    @JsonIgnore
    @AssertTrue(message = "Chapter 소속 또는 Department 참여의 하는 일을 한 개 이상 입력해야 합니다.") public boolean isResponsibilityPresent() {
        return !chapterMemberships.isEmpty() || !departmentParticipations.isEmpty();
    }

    public UpdateUmcProductResponsibilitiesCommand toCommand(
        Long requesterMemberId,
        Long umcProductMemberId
    ) {
        return new UpdateUmcProductResponsibilitiesCommand(
            requesterMemberId,
            umcProductMemberId,
            chapterMemberships.stream().map(ChapterResponsibility::toCommand).toList(),
            departmentParticipations.stream().map(DepartmentResponsibility::toCommand).toList()
        );
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record ChapterResponsibility(
        @NotNull Long chapterMembershipId,
        @Size(max = 200) String responsibilityTitle,
        @Size(max = 1000) String responsibilityDescription
    ) {
        UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility toCommand() {
            return new UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility(
                chapterMembershipId,
                responsibilityTitle,
                responsibilityDescription
            );
        }
    }

    public record DepartmentResponsibility(
        @NotNull Long departmentParticipantId,
        @Size(max = 200) String responsibilityTitle,
        @Size(max = 1000) String responsibilityDescription
    ) {
        UpdateUmcProductResponsibilitiesCommand.DepartmentResponsibility toCommand() {
            return new UpdateUmcProductResponsibilitiesCommand.DepartmentResponsibility(
                departmentParticipantId,
                responsibilityTitle,
                responsibilityDescription
            );
        }
    }
}
