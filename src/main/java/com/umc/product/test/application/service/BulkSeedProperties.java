package com.umc.product.test.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Profile;

/**
 * 벌크 시더 설정 (seeder 프로파일 전용). 실행 시 --app.bulk-seed.* 인자로 덮어쓴다.
 *
 * @param memberCount        생성할 멤버(=챌린저) 수
 * @param pointsPerChallenger 챌린저당 상벌점 수
 * @param schedulesPerMember 멤버당 참여 스케줄 수
 * @param noticeGlobalCount  GLOBAL 공지 수
 * @param randomSeed         결정성 seed — 같은 seed 면 같은 데이터
 * @param sampleMemberIdCount seed.json 에 담을 멤버 id 샘플 수
 * @param seedJsonPath       seed.json 출력 경로
 */
@Profile("seeder")
@ConfigurationProperties(prefix = "app.bulk-seed")
public record BulkSeedProperties(
    @DefaultValue("100000") int memberCount,
    @DefaultValue("2") int pointsPerChallenger,
    @DefaultValue("2") int schedulesPerMember,
    @DefaultValue("20") int noticeGlobalCount,
    @DefaultValue("42") long randomSeed,
    @DefaultValue("5000") int sampleMemberIdCount,
    @DefaultValue("./bulk-seed.json") String seedJsonPath
) {
}
