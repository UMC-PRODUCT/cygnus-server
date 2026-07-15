package com.umc.product.organization.application.port.in.command;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductChapterCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductChapterCommand;

public interface ManageUmcProductChapterUseCase {

    Long create(CreateUmcProductChapterCommand command);

    void update(UpdateUmcProductChapterCommand command);

    void delete(Long chapterId, Long requesterMemberId);
}
