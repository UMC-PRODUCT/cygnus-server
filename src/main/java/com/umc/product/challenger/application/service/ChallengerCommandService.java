package com.umc.product.challenger.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ChallengerDeactivationType;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.challenger.application.port.in.command.dto.DeactivateChallengerCommand;
import com.umc.product.challenger.application.port.in.command.dto.DeleteChallengerCommand;
import com.umc.product.challenger.application.port.in.command.dto.DeleteChallengerPointCommand;
import com.umc.product.challenger.application.port.in.command.dto.GrantChallengerPointCommand;
import com.umc.product.challenger.application.port.in.command.dto.UpdateChallengerCommand;
import com.umc.product.challenger.application.port.in.command.dto.UpdateChallengerPointCommand;
import com.umc.product.challenger.application.port.out.LoadChallengerPointPort;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPointPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengerCommandService implements ManageChallengerUseCase {

    private final Environment environment;

    private final LoadChallengerPort loadChallengerPort;
    private final SaveChallengerPort saveChallengerPort;
    private final LoadChallengerPointPort loadChallengerPointPort;
    private final SaveChallengerPointPort saveChallengerPointPort;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    // NOTE: 같은 도메인은 port를 통해서 접근하도록 함.
    // 동일 도메인 내에서 UseCase를 통해서 접근할 경우, 의존 방향이 역전된 것

    @Override
    public Long createChallenger(CreateChallengerCommand command) {
        // 동일 기수에 이미 등록된 챌린저인지 확인
        loadChallengerPort.findByMemberIdAndGisuId(command.memberId(), command.gisuId())
            .ifPresent(challenger -> {
                throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS);
            });

        Challenger challenger = new Challenger(
            command.memberId(),
            command.part(),
            command.gisuId()
        );

        Challenger savedChallenger = saveChallengerPort.save(challenger);
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(savedChallenger.getMemberId());
        return savedChallenger.getId();
    }

    /**
     * 대량의 챌린저를 한 번에 생성합니다.
     * <p>
     * 더미 데이터 생성 용으로 검증을 따로 진행하지 않으니 프로덕션에서 사용하고자 한다면 반드시 검증을 추가한 후 사용해주세요.
     */
    @Override
    public List<Long> createChallengerBulk(List<CreateChallengerCommand> commands) {
        // TODO: Dev 환경에서만 사용할 것, Prod에서 사용하고자 하는 경우 반드시 검증 로직을 추가하세요.
        validateEnvIsNotProduction();

        List<Challenger> challengers = commands.stream()
            .map(command -> new Challenger(
                command.memberId(),
                command.part(),
                command.gisuId()
            ))
            .toList();

        List<Challenger> savedChallengers = saveChallengerPort.saveAll(challengers);
        evictAuthoritySnapshotCacheUseCase.evictByMemberIds(savedChallengers.stream()
            .map(Challenger::getMemberId)
            .toList());

        return savedChallengers.stream()
            .map(Challenger::getId)
            .toList();
    }

    @Override
    public void updateChallenger(UpdateChallengerCommand command) {
        Challenger challenger = loadChallengerPort.getById(command.challengerId());

        // 변경이 필요한 내용이 둘 다 있는 경우 에러를 표시하도록 함
        if (command.newPart() == null && command.newStatus() == null) {
            throw new ChallengerDomainException(ChallengerErrorCode.BAD_CHALLENGER_UPDATE_REQUEST);
        }

        if (command.newPart() != null) {
            challenger.changePart(command.newPart());
        }

        if (command.newStatus() != null) {
            challenger.changeStatus(
                command.newStatus(),
                command.modifiedBy(),
                command.reason()
            );
        }

        saveChallengerPort.save(challenger);
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(challenger.getMemberId());
    }

    @Override
    public void deleteChallenger(DeleteChallengerCommand command) {
        Challenger challenger = loadChallengerPort.getById(command.challengerId());
        saveChallengerPort.delete(challenger);
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(challenger.getMemberId());
    }

    @Override
    public void deactivateChallenger(DeactivateChallengerCommand command) {
        Challenger challenger = loadChallengerPort.getById(command.challengerId());
        challenger.changeStatus(
            resolveDeactivationStatus(command.deactivationType()),
            command.modifiedBy(),
            command.reason()
        );
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(challenger.getMemberId());
    }

    @Override
    public void grantChallengerPoint(GrantChallengerPointCommand command) {
        Challenger challenger = loadChallengerPort.getById(command.challengerId());
        challenger.validateChallengerStatus();

        ChallengerPoint point = ChallengerPoint.create(
            challenger,
            command.pointType(),
            command.pointValue(),
            command.description()
        );

        challenger.addPoint(point);
        saveChallengerPort.save(challenger);
    }

    @Override
    public void grantChallengerPointBulk(List<GrantChallengerPointCommand> commands) {
        validateEnvIsNotProduction();

        List<Challenger> challengers = loadChallengerPort.getAllByIds(
            commands.stream()
                .map(GrantChallengerPointCommand::challengerId)
                .collect(Collectors.toSet())
        );

        for (Challenger challenger : challengers) {
            List<GrantChallengerPointCommand> challengerCommands = commands.stream()
                .filter(cmd -> cmd.challengerId().equals(challenger.getId()))
                .toList();

            challenger.validateChallengerStatus();

            for (GrantChallengerPointCommand command : challengerCommands) {
                ChallengerPoint point = ChallengerPoint.create(
                    challenger,
                    command.pointType(),
                    command.description()
                );

                challenger.addPoint(point);
            }
        }

        saveChallengerPort.saveAll(challengers);
    }

    @Override
    public void updateChallengerPoint(UpdateChallengerPointCommand command) {
        ChallengerPoint point = loadChallengerPointPort.getById(command.challengerPointId());
        point.updateDescription(command.newDescription());
        saveChallengerPointPort.save(point);
    }

    @Override
    public void deleteChallengerPoint(DeleteChallengerPointCommand command) {
        ChallengerPoint point = loadChallengerPointPort.getById(command.challengerPointId());
        saveChallengerPointPort.delete(point);
    }

    private ChallengerStatus resolveDeactivationStatus(ChallengerDeactivationType deactivationType) {
        return switch (deactivationType) {
            case WITHDRAW -> ChallengerStatus.WITHDRAWN;
            case EXPEL -> ChallengerStatus.EXPELLED;
        };
    }

    private void validateEnvIsNotProduction() {
        if (List.of(environment.getActiveProfiles()).contains("prod")) {
            throw new CommonException(CommonErrorCode.INVALID_ENV);
        }
    }
}
