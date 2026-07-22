package com.umc.product.test.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerRequest;
import com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerRoleRequest;
import com.umc.product.test.adapter.in.web.dto.CreateSeedMemberRequest;
import com.umc.product.test.adapter.in.web.dto.DeleteSeedProjectDataRequest;
import com.umc.product.test.adapter.in.web.dto.SeedChallengersRequest;
import com.umc.product.test.adapter.in.web.dto.SeedCurriculumRequest;
import com.umc.product.test.adapter.in.web.dto.SeedMembersRequest;
import com.umc.product.test.adapter.in.web.dto.SeedNoticeRequest;
import com.umc.product.test.adapter.in.web.dto.SeedProjectApplicationsRequest;
import com.umc.product.test.adapter.in.web.dto.SeedProjectScenariosRequest;
import com.umc.product.test.adapter.in.web.dto.SeedProjectsRequest;
import com.umc.product.test.application.port.in.command.CreateSeedChallengerRoleUseCase;
import com.umc.product.test.application.port.in.command.CreateSeedChallengerUseCase;
import com.umc.product.test.application.port.in.command.CreateSeedMemberUseCase;
import com.umc.product.test.application.port.in.command.DeleteSeedProjectDataUseCase;
import com.umc.product.test.application.port.in.command.SeedChallengersUseCase;
import com.umc.product.test.application.port.in.command.SeedCurriculumUseCase;
import com.umc.product.test.application.port.in.command.SeedMembersUseCase;
import com.umc.product.test.application.port.in.command.SeedNoticeUseCase;
import com.umc.product.test.application.port.in.command.SeedProjectApplicationsUseCase;
import com.umc.product.test.application.port.in.command.SeedProjectScenariosUseCase;
import com.umc.product.test.application.port.in.command.SeedProjectsUseCase;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerCommand;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerResult;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerRoleCommand;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerRoleResult;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberCommand;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberResult;
import com.umc.product.test.application.port.in.command.dto.DeleteSeedProjectDataCommand;
import com.umc.product.test.application.port.in.command.dto.DeleteSeedProjectDataResult;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersCommand;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersResult;
import com.umc.product.test.application.port.in.command.dto.SeedCurriculumCommand;
import com.umc.product.test.application.port.in.command.dto.SeedCurriculumResult;
import com.umc.product.test.application.port.in.command.dto.SeedMembersCommand;
import com.umc.product.test.application.port.in.command.dto.SeedMembersResult;
import com.umc.product.test.application.port.in.command.dto.SeedNoticeCommand;
import com.umc.product.test.application.port.in.command.dto.SeedNoticeResult;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsResult;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult;
import com.umc.product.test.application.port.in.command.dto.SeedProjectsCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectsResult;

@ExtendWith(MockitoExtension.class)
class SeedControllerTest {

    @Mock SeedMembersUseCase seedMembersUseCase;
    @Mock CreateSeedMemberUseCase createSeedMemberUseCase;
    @Mock SeedChallengersUseCase seedChallengersUseCase;
    @Mock CreateSeedChallengerUseCase createSeedChallengerUseCase;
    @Mock CreateSeedChallengerRoleUseCase createSeedChallengerRoleUseCase;
    @Mock SeedProjectsUseCase seedProjectsUseCase;
    @Mock DeleteSeedProjectDataUseCase deleteSeedProjectDataUseCase;
    @Mock SeedProjectScenariosUseCase seedProjectScenariosUseCase;
    @Mock SeedProjectApplicationsUseCase seedProjectApplicationsUseCase;
    @Mock SeedCurriculumUseCase seedCurriculumUseCase;
    @Mock SeedNoticeUseCase seedNoticeUseCase;

    SeedController sut;

    @BeforeEach
    void setUp() {
        sut = new SeedController(
            seedMembersUseCase,
            createSeedMemberUseCase,
            seedChallengersUseCase,
            createSeedChallengerUseCase,
            createSeedChallengerRoleUseCase,
            seedProjectsUseCase,
            deleteSeedProjectDataUseCase,
            seedProjectScenariosUseCase,
            seedProjectApplicationsUseCase,
            seedCurriculumUseCase,
            seedNoticeUseCase
        );
    }

    @Test
    @DisplayName("멤버·챌린저·역할 seed API는 변환한 Command를 위임한다")
    void 멤버와_챌린저_API_위임() {
        SeedMembersRequest membersRequest = mock(SeedMembersRequest.class);
        SeedMembersCommand membersCommand = mock(SeedMembersCommand.class);
        given(membersRequest.toCommand()).willReturn(membersCommand);
        given(seedMembersUseCase.seed(membersCommand)).willReturn(mock(SeedMembersResult.class));
        CreateSeedMemberRequest memberRequest = mock(CreateSeedMemberRequest.class);
        CreateSeedMemberCommand memberCommand = mock(CreateSeedMemberCommand.class);
        given(memberRequest.toCommand()).willReturn(memberCommand);
        given(createSeedMemberUseCase.create(memberCommand)).willReturn(mock(CreateSeedMemberResult.class));
        SeedChallengersRequest challengersRequest = mock(SeedChallengersRequest.class);
        SeedChallengersCommand challengersCommand = mock(SeedChallengersCommand.class);
        given(challengersRequest.toCommand()).willReturn(challengersCommand);
        given(seedChallengersUseCase.seed(challengersCommand)).willReturn(mock(SeedChallengersResult.class));
        CreateSeedChallengerRequest challengerRequest = mock(CreateSeedChallengerRequest.class);
        CreateSeedChallengerCommand challengerCommand = mock(CreateSeedChallengerCommand.class);
        given(challengerRequest.toCommand()).willReturn(challengerCommand);
        given(createSeedChallengerUseCase.create(challengerCommand))
            .willReturn(mock(CreateSeedChallengerResult.class));
        CreateSeedChallengerRoleRequest roleRequest = mock(CreateSeedChallengerRoleRequest.class);
        CreateSeedChallengerRoleCommand roleCommand = mock(CreateSeedChallengerRoleCommand.class);
        given(roleRequest.toCommand()).willReturn(roleCommand);
        given(createSeedChallengerRoleUseCase.create(roleCommand))
            .willReturn(mock(CreateSeedChallengerRoleResult.class));

        assertThat(sut.seedMembers(membersRequest)).isNotNull();
        assertThat(sut.createMember(memberRequest)).isNotNull();
        assertThat(sut.seedChallengers(challengersRequest)).isNotNull();
        assertThat(sut.createChallenger(challengerRequest)).isNotNull();
        assertThat(sut.createChallengerRole(roleRequest)).isNotNull();

        verify(seedMembersUseCase).seed(membersCommand);
        verify(createSeedMemberUseCase).create(memberCommand);
        verify(seedChallengersUseCase).seed(challengersCommand);
        verify(createSeedChallengerUseCase).create(challengerCommand);
        verify(createSeedChallengerRoleUseCase).create(roleCommand);
    }

    @Test
    @DisplayName("프로젝트·커리큘럼·공지 seed API는 변환한 Command를 위임한다")
    void 도메인_seed_API_위임() {
        SeedProjectsRequest projectsRequest = mock(SeedProjectsRequest.class);
        SeedProjectsCommand projectsCommand = mock(SeedProjectsCommand.class);
        given(projectsRequest.toCommand()).willReturn(projectsCommand);
        given(seedProjectsUseCase.seed(projectsCommand)).willReturn(mock(SeedProjectsResult.class));
        SeedProjectScenariosRequest scenariosRequest = mock(SeedProjectScenariosRequest.class);
        SeedProjectScenariosCommand scenariosCommand = mock(SeedProjectScenariosCommand.class);
        given(scenariosRequest.toCommand()).willReturn(scenariosCommand);
        given(seedProjectScenariosUseCase.seed(scenariosCommand))
            .willReturn(mock(SeedProjectScenariosResult.class));
        SeedProjectApplicationsRequest applicationsRequest = mock(SeedProjectApplicationsRequest.class);
        SeedProjectApplicationsCommand applicationsCommand = mock(SeedProjectApplicationsCommand.class);
        given(applicationsRequest.toCommand()).willReturn(applicationsCommand);
        SeedProjectApplicationsResult applicationsResult = mock(SeedProjectApplicationsResult.class);
        given(applicationsResult.counts()).willReturn(new SeedProjectApplicationsResult.Counts(0, 0, 0, 0));
        given(seedProjectApplicationsUseCase.seed(applicationsCommand)).willReturn(applicationsResult);
        SeedCurriculumRequest curriculumRequest = mock(SeedCurriculumRequest.class);
        SeedCurriculumCommand curriculumCommand = mock(SeedCurriculumCommand.class);
        given(curriculumRequest.toCommand()).willReturn(curriculumCommand);
        given(seedCurriculumUseCase.seed(curriculumCommand)).willReturn(mock(SeedCurriculumResult.class));
        SeedNoticeRequest noticeRequest = mock(SeedNoticeRequest.class);
        SeedNoticeCommand noticeCommand = mock(SeedNoticeCommand.class);
        given(noticeRequest.toCommand()).willReturn(noticeCommand);
        given(seedNoticeUseCase.seed(noticeCommand)).willReturn(mock(SeedNoticeResult.class));

        assertThat(sut.seedProjects(projectsRequest)).isNotNull();
        assertThat(sut.seedProjectScenarios(scenariosRequest)).isNotNull();
        assertThat(sut.seedProjectApplications(applicationsRequest)).isNotNull();
        assertThat(sut.seedCurriculum(curriculumRequest)).isNotNull();
        assertThat(sut.seedNotice(noticeRequest)).isNotNull();

        verify(seedProjectsUseCase).seed(projectsCommand);
        verify(seedProjectScenariosUseCase).seed(scenariosCommand);
        verify(seedProjectApplicationsUseCase).seed(applicationsCommand);
        verify(seedCurriculumUseCase).seed(curriculumCommand);
        verify(seedNoticeUseCase).seed(noticeCommand);
    }

    @Test
    @DisplayName("프로젝트 삭제 요청이 없으면 활성 기수를 뜻하는 null Command를 사용한다")
    void 프로젝트_삭제_null_요청_처리() {
        DeleteSeedProjectDataRequest request = mock(DeleteSeedProjectDataRequest.class);
        DeleteSeedProjectDataCommand explicitCommand = DeleteSeedProjectDataCommand.of(9L);
        given(request.toCommand()).willReturn(explicitCommand);
        given(deleteSeedProjectDataUseCase.delete(org.mockito.ArgumentMatchers.any()))
            .willReturn(mock(DeleteSeedProjectDataResult.class));

        assertThat(sut.deleteProjectData(null)).isNotNull();
        assertThat(sut.deleteProjectData(request)).isNotNull();

        ArgumentCaptor<DeleteSeedProjectDataCommand> captor =
            ArgumentCaptor.forClass(DeleteSeedProjectDataCommand.class);
        verify(deleteSeedProjectDataUseCase, times(2)).delete(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(DeleteSeedProjectDataCommand::gisuId)
            .containsExactly(null, 9L);
    }
}
