package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductChapterMembershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductDepartmentParticipationCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductLeadershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberCommand;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUmcProductMemberRequest(
    @NotBlank @Size(max = 30) String name,
    @NotBlank @Size(max = 20) String nickname,
    @NotBlank @Pattern(regexp = "^[a-z0-9._-]{2,30}$") String englishNickname,
    Long schoolId,
    @Size(max = 2000) String introduction,
    String profileImageId,
    @NotEmpty List<@NotNull @Valid UmcProductActivityPeriodRequest> activityPeriods,
    List<@NotNull @Valid ChapterMembership> chapterMemberships,
    List<@NotNull @Valid DepartmentParticipation> departmentParticipations,
    List<@NotNull @Valid ProductLeadership> productLeaderships
) {
    public RegisterUmcProductMemberRequest {
        activityPeriods = immutable(activityPeriods);
        chapterMemberships = immutable(chapterMemberships);
        departmentParticipations = immutable(departmentParticipations);
        productLeaderships = immutable(productLeaderships);
    }

    public RegisterUmcProductMemberCommand toCommand(Long requesterMemberId) {
        return new RegisterUmcProductMemberCommand(
            requesterMemberId,
            name,
            nickname,
            englishNickname,
            schoolId,
            introduction,
            profileImageId,
            activityPeriods.stream().map(UmcProductActivityPeriodRequest::toCommand).toList(),
            chapterMemberships.stream().map(ChapterMembership::toCommand).toList(),
            departmentParticipations.stream().map(DepartmentParticipation::toCommand).toList(),
            productLeaderships.stream().map(ProductLeadership::toCommand).toList()
        );
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record ChapterMembership(
        @NotNull Long chapterId,
        @NotNull UmcProductPosition position,
        @Size(max = 200) String responsibilityTitle,
        @Size(max = 1000) String responsibilityDescription,
        @NotNull @UmcProductDateFormat @Schema(type = "string", format = "date") LocalDate startDate,
        @UmcProductDateFormat @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
    ) {
        RegisterUmcProductChapterMembershipCommand toCommand() {
            return new RegisterUmcProductChapterMembershipCommand(
                chapterId,
                position,
                responsibilityTitle,
                responsibilityDescription,
                startDate,
                endDate
            );
        }
    }

    public record DepartmentParticipation(
        @NotNull Long departmentId,
        @NotNull UmcProductDepartmentRole role,
        @NotNull UmcProductPosition position,
        @Size(max = 200) String responsibilityTitle,
        @Size(max = 1000) String responsibilityDescription,
        @NotNull @UmcProductDateFormat @Schema(type = "string", format = "date") LocalDate startDate,
        @UmcProductDateFormat @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
    ) {
        RegisterUmcProductDepartmentParticipationCommand toCommand() {
            return new RegisterUmcProductDepartmentParticipationCommand(
                departmentId,
                role,
                position,
                responsibilityTitle,
                responsibilityDescription,
                startDate,
                endDate
            );
        }
    }

    public record ProductLeadership(
        @NotNull UmcProductLeadershipRole role,
        @NotNull @UmcProductDateFormat @Schema(type = "string", format = "date") LocalDate startDate,
        @UmcProductDateFormat @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
    ) {
        RegisterUmcProductLeadershipCommand toCommand() {
            return new RegisterUmcProductLeadershipCommand(role, startDate, endDate);
        }
    }
}
