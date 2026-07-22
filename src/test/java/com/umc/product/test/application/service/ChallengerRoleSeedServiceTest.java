package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerRoleCommand;

@ExtendWith(MockitoExtension.class)
class ChallengerRoleSeedServiceTest {

    @Mock ManageChallengerRoleUseCase manageChallengerRoleUseCase;

    @Test
    @DisplayName("seed 역할 생성은 scope와 담당 파트를 authorization Command에 그대로 전달한다")
    void 역할_생성_위임() {
        ChallengerRoleSeedService sut = new ChallengerRoleSeedService(manageChallengerRoleUseCase);
        CreateSeedChallengerRoleCommand command = CreateSeedChallengerRoleCommand.of(
            1L, ChallengerRoleType.SCHOOL_PART_LEADER, 2L, ChallengerPart.WEB, 9L
        );
        ArgumentCaptor<CreateChallengerRoleCommand> captor =
            ArgumentCaptor.forClass(CreateChallengerRoleCommand.class);
        given(manageChallengerRoleUseCase.createChallengerRole(captor.capture())).willReturn(100L);

        var result = sut.create(command);

        assertThat(result.challengerRoleId()).isEqualTo(100L);
        assertThat(result.challengerId()).isEqualTo(1L);
        assertThat(result.roleType()).isEqualTo(ChallengerRoleType.SCHOOL_PART_LEADER);
        assertThat(result.organizationId()).isEqualTo(2L);
        assertThat(result.responsiblePart()).isEqualTo(ChallengerPart.WEB);
        assertThat(result.gisuId()).isEqualTo(9L);
        assertThat(captor.getValue().challengerId()).isEqualTo(1L);
        assertThat(captor.getValue().organizationId()).isEqualTo(2L);
    }
}
