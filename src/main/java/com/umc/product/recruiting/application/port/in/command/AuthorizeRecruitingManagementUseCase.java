package com.umc.product.recruiting.application.port.in.command;

public interface AuthorizeRecruitingManagementUseCase {

    void authorizeSeasonManagement(Long requesterMemberId, Long seasonId);

    boolean canManageSeason(Long requesterMemberId, Long seasonId);
}
