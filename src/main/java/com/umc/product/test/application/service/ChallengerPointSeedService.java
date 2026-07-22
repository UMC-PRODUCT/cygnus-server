package com.umc.product.test.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.GrantChallengerPointCommand;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.test.application.port.in.command.SeedChallengerPointsUseCase;
import com.umc.product.test.application.port.in.command.dto.SeedChallengerPointsCommand;
import com.umc.product.test.application.port.in.command.dto.SeedChallengerPointsResult;

import lombok.RequiredArgsConstructor;

@Service
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ChallengerPointSeedService implements SeedChallengerPointsUseCase {

    // 홈 화면 challengerHistory 에 상점/벌점이 섞여 보이도록 순환 부여하는 타입 셋 (상점 3 + 벌점 2)
    private static final List<PointType> CYCLE_POINT_TYPES = List.of(
        PointType.BLOG_CHALLENGE,
        PointType.PEER_REVIEW_SUBMISSION,
        PointType.BEST_WORKBOOK_V2,
        PointType.STUDY_LATE,
        PointType.NO_WORKBOOK_MISSION
    );
    private static final String DEFAULT_DESCRIPTION = "시딩 상벌점";

    private final ManageChallengerUseCase manageChallengerUseCase;

    @Override
    @Transactional
    public SeedChallengerPointsResult seed(SeedChallengerPointsCommand command) {
        String description = (command.description() == null || command.description().isBlank())
            ? DEFAULT_DESCRIPTION
            : command.description();

        List<GrantChallengerPointCommand> grants = new ArrayList<>();
        for (Long challengerId : command.challengerIds()) {
            for (int i = 0; i < command.countPerChallenger(); i++) {
                grants.add(GrantChallengerPointCommand.builder()
                    .challengerId(challengerId)
                    .pointType(CYCLE_POINT_TYPES.get(grants.size() % CYCLE_POINT_TYPES.size()))
                    .description(description)
                    .build());
            }
        }

        manageChallengerUseCase.grantChallengerPointBulk(grants);

        return SeedChallengerPointsResult.of(command.challengerIds().size(), grants.size());
    }
}
