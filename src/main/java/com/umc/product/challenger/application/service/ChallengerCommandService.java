package com.umc.product.challenger.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.challenger.application.port.in.command.AddChallengerTrackUseCase;
import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.AddChallengerTrackCommand;
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
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengerCommandService implements ManageChallengerUseCase, AddChallengerTrackUseCase {

    private final Environment environment;

    private final LoadChallengerPort loadChallengerPort;
    private final SaveChallengerPort saveChallengerPort;
    private final LoadChallengerPointPort loadChallengerPointPort;
    private final SaveChallengerPointPort saveChallengerPointPort;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;
    private final GetGisuUseCase getGisuUseCase;

    // NOTE: 같은 도메인은 port를 통해서 접근하도록 함.
    // 동일 도메인 내에서 UseCase를 통해서 접근할 경우, 의존 방향이 역전된 것

    @Override
    public Long createChallenger(CreateChallengerCommand command) {
        // 동일 기수에 이미 등록된 챌린저인지 확인
        loadChallengerPort.findByMemberIdAndGisuId(command.memberId(), command.gisuId())
            .ifPresent(challenger -> {
                throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS);
            });

        Challenger challenger = createChallengerForGisu(command);

        Challenger savedChallenger = saveChallengerPort.save(challenger);
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(savedChallenger.getMemberId());
        return savedChallenger.getId();
    }

    private Challenger createChallengerForGisu(CreateChallengerCommand command) {
        if (getGisuUseCase.getById(command.gisuId()).learningType() == GisuLearningType.TRACK) {
            if (command.part() != null
                || command.tracks().stream().anyMatch(track -> track == null || !track.isBasic())) {
                throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_LEARNING_TYPE);
            }
            if (command.tracks().isEmpty()) {
                return Challenger.createWithoutEnrollment(command.memberId(), command.gisuId());
            }
        }
        return Challenger.builder()
            .memberId(command.memberId())
            .part(command.part())
            .tracks(command.tracks())
            .gisuId(command.gisuId())
            .build();
    }

    /**
     * 대량의 챌린저를 한 번에 생성합니다.
     * <p>
     * 더미 데이터 생성용이며 기수별 학습 유형만 검증합니다. 프로덕션에서 사용하려면 중복 등 추가 검증이 필요합니다.
     */
    @Override
    public List<Long> createChallengerBulk(List<CreateChallengerCommand> commands) {
        // TODO: Dev 환경에서만 사용할 것, Prod에서 사용하고자 하는 경우 반드시 검증 로직을 추가하세요.
        validateEnvIsNotProduction();
        List<Challenger> challengers = commands.stream()
            .map(this::createChallengerForGisu)
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
    public void addTrack(AddChallengerTrackCommand command) {
        Challenger challenger = loadChallengerPort.findByMemberIdAndGisuId(command.memberId(), command.gisuId())
            .orElseGet(() -> Challenger.builder()
                .memberId(command.memberId())
                .tracks(List.of(command.track()))
                .gisuId(command.gisuId())
                .build());

        if (challenger.getId() == null || challenger.addTrack(command.track())) {
            saveChallengerPort.save(challenger);
            evictAuthoritySnapshotCacheUseCase.evictByMemberId(command.memberId());
        }
    }

    @Override
    public void updateChallenger(UpdateChallengerCommand command) {
        Challenger challenger = loadChallengerPort.getById(command.challengerId());

        // 변경이 필요한 내용이 둘 다 있는 경우 에러를 표시하도록 함
        if (command.newPart() == null && command.newStatus() == null) {
            throw new ChallengerDomainException(ChallengerErrorCode.BAD_CHALLENGER_UPDATE_REQUEST);
        }

        if (command.newPart() != null) {
            if (getGisuUseCase.getById(challenger.getGisuId()).learningType() == GisuLearningType.TRACK) {
                throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_LEARNING_TYPE);
            }
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
        saveChallengerPointPort.deleteAllByChallengerId(challenger.getId());
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

        saveChallengerPointPort.save(point);
    }

    @Override
    public void grantChallengerPointBulk(List<GrantChallengerPointCommand> commands) {
        validateEnvIsNotProduction();

        Map<Long, List<GrantChallengerPointCommand>> commandsByChallengerId = commands.stream()
            .collect(Collectors.groupingBy(GrantChallengerPointCommand::challengerId));
        List<Challenger> challengers = loadChallengerPort.getAllByIds(commandsByChallengerId.keySet());
        List<ChallengerPoint> points = new ArrayList<>();

        for (Challenger challenger : challengers) {
            challenger.validateChallengerStatus();

            for (GrantChallengerPointCommand command : commandsByChallengerId.getOrDefault(
                challenger.getId(),
                List.of()
            )) {
                points.add(ChallengerPoint.create(
                    challenger,
                    command.pointType(),
                    command.description()
                ));
            }
        }

        saveChallengerPointPort.saveAll(points);
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
