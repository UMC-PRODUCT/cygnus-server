package com.umc.product.project.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.project.application.port.in.command.AddProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.ChangeProjectMemberStatusUseCase;
import com.umc.product.project.application.port.in.command.RemoveProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.dto.AddProjectMemberCommand;
import com.umc.product.project.application.port.in.command.dto.ChangeProjectMemberStatusCommand;
import com.umc.product.project.application.port.in.command.dto.RemoveProjectMemberCommand;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.SaveProjectMemberPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectMemberCommandService
    implements AddProjectMemberUseCase, RemoveProjectMemberUseCase, ChangeProjectMemberStatusUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final SaveProjectMemberPort saveProjectMemberPort;

    @Override
    public Long add(AddProjectMemberCommand command) {
        Project project = loadProjectPort.getById(command.projectId());
        project.validateMutable();

        loadProjectMemberPort.findByProjectIdAndMemberId(command.projectId(), command.memberId())
            .ifPresent(existing -> {
                throw new ProjectDomainException(ProjectErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
            });

        ProjectMember member = ProjectMember.create(
            project, command.memberId(), command.part(), command.requesterMemberId());
        return saveProjectMemberPort.save(member).getId();
    }

    /**
     * 멤버 hard delete. 행을 물리적으로 삭제해 동일 멤버의 재등록을 허용한다.
     * <ul>
     *   <li>DRAFT / PENDING_REVIEW / IN_PROGRESS: hard delete</li>
     *   <li>COMPLETED / ABORTED: 거부 — 종료된 프로젝트의 참여 이력 보존</li>
     * </ul>
     * 메인 PM 은 양도 API 로 변경해야 하므로 본 메서드에서 거부한다.
     * 행과 함께 사라지는 {@code reason} 은 삭제 직전 감사 로그로만 남긴다.
     */
    @Override
    public void remove(RemoveProjectMemberCommand command) {
        Project project = loadProjectPort.getById(command.projectId());

        if (project.isOwner(command.memberId())) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MAIN_PM_REMOVAL_REQUIRES_TRANSFER);
        }

        // COMPLETED / ABORTED 거부. 추후 ABORTED 만 허용하려면 이 가드를 COMPLETED 단독 검사로 교체한다.
        project.validateMutable();

        ProjectMember member = loadProjectMemberPort
            .getByProjectIdAndMemberId(command.projectId(), command.memberId());

        log.info(
            "[ProjectMember hardDelete] projectId={}, memberId={}, projectMemberId={}, requesterMemberId={}, reason={}",
            command.projectId(), command.memberId(), member.getId(), command.requesterMemberId(), command.reason());

        saveProjectMemberPort.hardDelete(member.getId());
    }

    /**
     * 멤버 상태 변경 (soft delete). 행을 보존한 채 {@code status} 만 바꾸고 사유·주체를 기록한다.
     * <ul>
     *   <li>메인 PM: 거부 — 양도 API 로 유도</li>
     *   <li>COMPLETED / ABORTED 프로젝트: 거부 — 종료된 프로젝트의 이력 고정</li>
     * </ul>
     * 재등록이 필요하면 hard delete API({@code PROJECT-005}) 를 사용한다.
     */
    @Override
    public void changeStatus(ChangeProjectMemberStatusCommand command) {
        Project project = loadProjectPort.getById(command.projectId());

        if (project.isOwner(command.memberId())) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MAIN_PM_REMOVAL_REQUIRES_TRANSFER);
        }

        project.validateMutable();

        ProjectMember member = loadProjectMemberPort
            .getByProjectIdAndMemberId(command.projectId(), command.memberId());

        member.changeStatus(command.status(), command.requesterMemberId(), command.reason());
        saveProjectMemberPort.save(member);
    }
}
