package com.umc.product.organization.application.port.in.command;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductPartMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductPartMembershipCommand;

public interface ManageUmcProductMemberUseCase {

    Long create(CreateUmcProductMemberCommand command);

    void updateProfile(UpdateUmcProductMemberProfileCommand command);

    void delete(Long umcProductMemberId, Long requesterMemberId);

    Long createActivityPeriod(CreateUmcProductMemberActivityPeriodCommand command);

    void updateActivityPeriod(UpdateUmcProductMemberActivityPeriodCommand command);

    void deleteActivityPeriod(Long umcProductMemberId, Long activityPeriodId, Long requesterMemberId);

    Long createPartMembership(CreateUmcProductPartMembershipCommand command);

    void updatePartMembership(UpdateUmcProductPartMembershipCommand command);

    void deletePartMembership(Long umcProductMemberId, Long partMembershipId, Long requesterMemberId);

    Long createLeadership(CreateUmcProductLeadershipCommand command);

    void updateLeadership(UpdateUmcProductLeadershipCommand command);

    void deleteLeadership(Long umcProductMemberId, Long leadershipId, Long requesterMemberId);
}
