package com.umc.product.organization.application.port.in.command;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductChapterMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductChapterMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;

public interface ManageUmcProductMemberUseCase {

    Long create(CreateUmcProductMemberCommand command);

    void updateProfile(UpdateUmcProductMemberProfileCommand command);

    void delete(Long umcProductMemberId, Long requesterMemberId);

    Long createActivityPeriod(CreateUmcProductMemberActivityPeriodCommand command);

    void updateActivityPeriod(UpdateUmcProductMemberActivityPeriodCommand command);

    void deleteActivityPeriod(Long umcProductMemberId, Long activityPeriodId, Long requesterMemberId);

    Long createChapterMembership(CreateUmcProductChapterMembershipCommand command);

    void updateChapterMembership(UpdateUmcProductChapterMembershipCommand command);

    void deleteChapterMembership(Long umcProductMemberId, Long chapterMembershipId, Long requesterMemberId);

    Long createLeadership(CreateUmcProductLeadershipCommand command);

    void updateLeadership(UpdateUmcProductLeadershipCommand command);

    void deleteLeadership(Long umcProductMemberId, Long leadershipId, Long requesterMemberId);
}
