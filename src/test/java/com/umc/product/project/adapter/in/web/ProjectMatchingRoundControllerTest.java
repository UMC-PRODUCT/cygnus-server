package com.umc.product.project.adapter.in.web;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.application.port.in.command.AutoDecideProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.CreateProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.DeleteProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMatchingRoundUseCase;

@ExtendWith(MockitoExtension.class)
class ProjectMatchingRoundControllerTest {

    @Mock
    GetProjectMatchingRoundUseCase getProjectMatchingRoundUseCase;
    @Mock
    CreateProjectMatchingRoundUseCase createProjectMatchingRoundUseCase;
    @Mock
    UpdateProjectMatchingRoundUseCase updateProjectMatchingRoundUseCase;
    @Mock
    DeleteProjectMatchingRoundUseCase deleteProjectMatchingRoundUseCase;
    @Mock
    AutoDecideProjectMatchingRoundUseCase autoDecideProjectMatchingRoundUseCase;

    @InjectMocks
    ProjectMatchingRoundController sut;

    @Test
    @DisplayName("삭제와 자동 선발은 매칭 차수와 현재 회원 ID를 전달한다")
    void delete_and_auto_decide_delegate_identifiers() {
        MemberPrincipal principal = new MemberPrincipal(99L);

        sut.delete(principal, 42L);
        sut.autoDecide(principal, 42L);

        then(deleteProjectMatchingRoundUseCase).should().delete(42L, 99L);
        then(autoDecideProjectMatchingRoundUseCase).should().autoDecide(42L, 99L);
    }
}
