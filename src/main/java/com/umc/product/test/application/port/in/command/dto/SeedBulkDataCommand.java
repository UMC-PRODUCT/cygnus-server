package com.umc.product.test.application.port.in.command.dto;

public record SeedBulkDataCommand(
    int memberCount,
    int pointsPerChallenger,
    int schedulesPerMember,
    int noticeGlobalCount,
    long randomSeed,
    int sampleMemberIdCount
) {
}
