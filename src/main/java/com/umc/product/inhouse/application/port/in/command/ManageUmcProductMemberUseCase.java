package com.umc.product.inhouse.application.port.in.command;

import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductChapterMembershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductLeadershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.inhouse.application.port.in.command.dto.LinkUmcProductMemberAccountCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberResult;
import com.umc.product.inhouse.application.port.in.command.dto.ResetUmcProductAccountPasswordResult;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductChapterMembershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductResponsibilitiesCommand;

public interface ManageUmcProductMemberUseCase {

    Long create(CreateUmcProductMemberCommand command);

    RegisterUmcProductMemberResult register(RegisterUmcProductMemberCommand command);

    Long linkAccount(LinkUmcProductMemberAccountCommand command);

    void unlinkAccount(Long umcProductMemberId, Long memberId, Long requesterMemberId);

    ResetUmcProductAccountPasswordResult resetAccountPassword(
        Long umcProductMemberId,
        Long memberId,
        Long requesterMemberId
    );

    void updateProfile(UpdateUmcProductMemberProfileCommand command);

    void updateResponsibilities(UpdateUmcProductResponsibilitiesCommand command);

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
