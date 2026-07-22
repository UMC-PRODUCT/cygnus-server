package com.umc.product.test.application.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.command.dto.UpdatePartQuotasCommand.Entry;

/**
 * IN_PROGRESS 시나리오의 PartQuota 분배 정책.
 * <p>
 * 한 프로젝트당 {@code DESIGN} × 1~2 + 프론트엔드 파트 1 개 × 3~4 + 백엔드 파트 1 개 × 3~4 를
 * 만든다. 프론트엔드는 {@code WEB, ANDROID, IOS}, 백엔드는 {@code NODEJS, SPRINGBOOT} 중
 * 균등 무작위로 선정한다. quota 는 모두 1 이상이라 {@code PROJECT_PART_QUOTA_INVALID} 가드를
 * 항상 통과한다.
 */
@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class ScenarioPartQuotaPolicy {

    static final long DESIGN_MIN = 1L;
    static final long DESIGN_MAX = 2L;
    static final long FE_MIN = 3L;
    static final long FE_MAX = 4L;
    static final long BE_MIN = 3L;
    static final long BE_MAX = 4L;

    private static final List<ChallengerPart> FRONTEND_PARTS = List.of(
        ChallengerPart.WEB,
        ChallengerPart.ANDROID,
        ChallengerPart.IOS
    );

    private static final List<ChallengerPart> BACKEND_PARTS = List.of(
        ChallengerPart.NODEJS,
        ChallengerPart.SPRINGBOOT
    );

    /**
     * 한 프로젝트의 partQuota 3 entry 를 만든다. 순서는 항상 {@code [DESIGN, FE, BE]}.
     */
    public List<Entry> pickQuotas() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long designQuota = randomInclusive(random, DESIGN_MIN, DESIGN_MAX);
        long feQuota = randomInclusive(random, FE_MIN, FE_MAX);
        long beQuota = randomInclusive(random, BE_MIN, BE_MAX);
        ChallengerPart fePart = FRONTEND_PARTS.get(random.nextInt(FRONTEND_PARTS.size()));
        ChallengerPart bePart = BACKEND_PARTS.get(random.nextInt(BACKEND_PARTS.size()));
        return List.of(
            Entry.builder().part(ChallengerPart.DESIGN).quota(designQuota).build(),
            Entry.builder().part(fePart).quota(feQuota).build(),
            Entry.builder().part(bePart).quota(beQuota).build()
        );
    }

    long randomInclusive(ThreadLocalRandom random, long min, long max) {
        if (max <= min) {
            return min;
        }
        return random.nextLong(min, max + 1);
    }
}
