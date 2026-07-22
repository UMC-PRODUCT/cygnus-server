package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.GrantChallengerPointCommand;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.test.application.port.in.command.dto.SeedChallengerPointsCommand;
import com.umc.product.test.application.port.in.command.dto.SeedChallengerPointsResult;

@ExtendWith(MockitoExtension.class)
class ChallengerPointSeedServiceTest {

    @Mock
    ManageChallengerUseCase manageChallengerUseCase;

    @Captor
    ArgumentCaptor<List<GrantChallengerPointCommand>> grantsCaptor;

    @InjectMocks
    ChallengerPointSeedService sut;

    @Test
    @DisplayName("챌린저별 countPerChallenger 건씩, 상점/벌점 타입을 순환하며 벌크 부여한다")
    void 상벌점_타입_순환_벌크_부여() {
        // Given
        SeedChallengerPointsCommand command =
            new SeedChallengerPointsCommand(List.of(1L, 2L, 3L), 2, "부하테스트 시딩");

        // When
        SeedChallengerPointsResult result = sut.seed(command);

        // Then
        verify(manageChallengerUseCase).grantChallengerPointBulk(grantsCaptor.capture());
        List<GrantChallengerPointCommand> grants = grantsCaptor.getValue();
        assertThat(grants).hasSize(6);
        assertThat(grants).extracting(GrantChallengerPointCommand::challengerId)
            .containsExactly(1L, 1L, 2L, 2L, 3L, 3L);
        assertThat(grants).extracting(GrantChallengerPointCommand::pointType)
            .containsExactly(
                PointType.BLOG_CHALLENGE, PointType.PEER_REVIEW_SUBMISSION,
                PointType.BEST_WORKBOOK_V2, PointType.STUDY_LATE,
                PointType.NO_WORKBOOK_MISSION, PointType.BLOG_CHALLENGE
            );
        assertThat(grants).allSatisfy(grant ->
            assertThat(grant.description()).isEqualTo("부하테스트 시딩"));
        assertThat(result.challengerCount()).isEqualTo(3);
        assertThat(result.grantedPointCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("description 이 비어 있으면 기본 설명으로 대체한다")
    void description_기본값_대체() {
        // Given
        SeedChallengerPointsCommand command = new SeedChallengerPointsCommand(List.of(1L), 1, null);

        // When
        sut.seed(command);

        // Then
        verify(manageChallengerUseCase).grantChallengerPointBulk(grantsCaptor.capture());
        assertThat(grantsCaptor.getValue())
            .extracting(GrantChallengerPointCommand::description)
            .containsExactly("시딩 상벌점");
    }
}
