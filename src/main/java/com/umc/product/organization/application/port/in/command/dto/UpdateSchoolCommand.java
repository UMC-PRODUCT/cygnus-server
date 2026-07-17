package com.umc.product.organization.application.port.in.command.dto;

import java.util.List;

public record UpdateSchoolCommand(
    Long requesterMemberId,
    String schoolName,
    Long chapterId,
    String remark,
    String logoImageId,
    List<CreateSchoolCommand.SchoolLinkCommand> links
) {
    public UpdateSchoolCommand(
        String schoolName,
        Long chapterId,
        String remark,
        String logoImageId,
        List<CreateSchoolCommand.SchoolLinkCommand> links
    ) {
        this(null, schoolName, chapterId, remark, logoImageId, links);
    }
}
