package com.umc.product.organization.application.port.in.command.dto;

import java.util.List;

import com.umc.product.organization.domain.School;
import com.umc.product.organization.domain.SchoolLink;
import com.umc.product.organization.domain.enums.SchoolLinkType;

import lombok.Builder;

@Builder
public record CreateSchoolCommand(
    Long requesterMemberId,
    String schoolName,
    String remark,
    String logoImageId,
    List<SchoolLinkCommand> links
) {
    public CreateSchoolCommand(
        String schoolName,
        String remark,
        String logoImageId,
        List<SchoolLinkCommand> links
    ) {
        this(null, schoolName, remark, logoImageId, links);
    }

    public CreateSchoolCommand {
        links = links != null ? links : List.of();
    }

    public static CreateSchoolCommand of(
        Long requesterMemberId,
        String schoolName,
        String remark,
        String logoImageId,
        List<SchoolLinkCommand> links
    ) {
        return new CreateSchoolCommand(requesterMemberId, schoolName, remark, logoImageId, links);
    }

    public record SchoolLinkCommand(
        String title,
        SchoolLinkType type,
        String url
    ) {
        public SchoolLink toEntity(School school) {
            return SchoolLink.create(school, title, type, url);
        }
    }
}
