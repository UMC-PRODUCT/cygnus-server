package com.umc.product.challenger.application.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.CheckChallengerHistoryUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfoWithStatus;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPointInfo;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengerQueryService implements GetChallengerUseCase, CheckChallengerHistoryUseCase {

    private final LoadChallengerPort loadChallengerPort;
    private final GetChallengerPointUseCase getChallengerPointUseCase;

    @Override
    public ChallengerInfo getById(Long challengerId) {
        return getChallengerInfoFromChallenger(loadChallengerPort.getById(challengerId));
    }

    @Override
    public Optional<ChallengerInfo> findById(Long challengerId) {
        return loadChallengerPort.findById(challengerId)
            .map(this::getChallengerInfoFromChallenger);
    }

    @Override
    public ChallengerInfo findByIdOrNull(Long challengerId) {
        Challenger challenger = loadChallengerPort.findById(challengerId).orElse(null);

        return challenger != null
            ? getChallengerInfoFromChallenger(challenger)
            : null;
    }

    @Override
    public ChallengerInfo getByMemberIdAndGisuId(Long memberId, Long gisuId) {
        Challenger challenger = loadChallengerPort.findByMemberIdAndGisuId(memberId, gisuId)
            .orElseThrow(() -> new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_NOT_FOUND));

        return getChallengerInfoFromChallenger(challenger);
    }

    @Override
    public Optional<ChallengerInfo> findByMemberIdAndGisuId(Long memberId, Long gisuId) {
        return loadChallengerPort.findByMemberIdAndGisuId(memberId, gisuId)
            .map(this::getChallengerInfoFromChallenger);
    }

    @Override
    public ChallengerInfo getActiveByMemberIdAndGisuId(Long memberId, Long gisuId) {
        Challenger challenger = loadChallengerPort.findByMemberIdAndGisuId(memberId, gisuId)
            .orElseThrow(() -> new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_NOT_FOUND));

        challenger.validateChallengerStatus();

        return getChallengerInfoFromChallenger(challenger);
    }

    @Override
    public List<ChallengerInfo> getAllByMemberId(Long memberId) {
        // 챌린저별로 상벌점을 따로 조회하면 N(챌린저 수) 만큼 쿼리가 발생하므로,
        // IN 쿼리 1회로 일괄 조회하는 batch 헬퍼를 사용합니다.
        return toChallengerInfoListBatch(loadChallengerPort.getAllByMemberId(memberId));
    }

    @Override
    public boolean hasChallengerHistory(Long memberId) {
        if (memberId == null) {
            return false;
        }
        return loadChallengerPort.existsByMemberId(memberId);
    }

    @Override
    public Map<Long, List<ChallengerInfo>> getAllByMemberIds(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }
        List<Challenger> challengers = loadChallengerPort.listAllByMemberIds(memberIds);
        if (challengers.isEmpty()) {
            return Map.of();
        }
        List<ChallengerInfo> infos = toChallengerInfoListBatch(challengers);
        return infos.stream()
            .collect(Collectors.groupingBy(ChallengerInfo::memberId));
    }

    @Override
    public Map<Long, List<ChallengerBasicInfo>> getAllBasicByMemberIds(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }
        List<Challenger> challengers = loadChallengerPort.listAllByMemberIds(memberIds);
        if (challengers.isEmpty()) {
            return Map.of();
        }
        return challengers.stream()
            .map(ChallengerBasicInfo::from)
            .collect(Collectors.groupingBy(ChallengerBasicInfo::memberId));
    }

    @Override
    public List<ChallengerBasicInfo> listBasicByMemberIdsAndGisuId(Set<Long> memberIds, Long gisuId) {
        return loadChallengerPort.listByMemberIdsAndGisuId(memberIds, gisuId).stream()
            .map(ChallengerBasicInfo::from)
            .toList();
    }

    @Override
    public List<ChallengerBasicInfo> listBasicByGisuId(Long gisuId) {
        return loadChallengerPort.getAllByGisuId(gisuId).stream()
            .map(ChallengerBasicInfo::from)
            .toList();
    }

    @Override
    public ChallengerInfoWithStatus getLatestActiveChallengerByMemberId(Long memberId) {
        Challenger challenger = loadChallengerPort.findTopByMemberIdOrderByCreatedAtDesc(memberId);
        if (challenger.getStatus() == ChallengerStatus.WITHDRAWN
            || challenger.getStatus() == ChallengerStatus.EXPELLED) {
            throw new ChallengerDomainException(ChallengerErrorCode.NOT_ALLOWED_AUTHOR);
        }
        return ChallengerInfoWithStatus.from(challenger);
    }

    @Override
    public Map<Long, ChallengerInfo> getAllByIdsAsMap(Set<Long> challengerIds) {
        if (challengerIds == null || challengerIds.isEmpty()) {
            return Map.of();
        }
        return loadChallengerPort.getAllByIds(challengerIds).stream()
            .collect(Collectors.toMap(
                Challenger::getId,
                this::getChallengerInfoFromChallenger
            ));
    }

    @Override
    public List<ChallengerInfo> getAllByIds(Set<Long> challengerIds) {
        if (challengerIds == null || challengerIds.isEmpty()) {
            return List.of();
        }
        return loadChallengerPort.getAllByIds(challengerIds).stream()
            .map(this::getChallengerInfoFromChallenger).toList();
    }

    @Override
    public Map<Long, ChallengerInfo> batchGetByMemberIdsAndGisuId(Set<Long> memberIds, Long gisuId) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }
        // Adapter 가 size 검증 후 던지므로 Service 에서 추가 검증 없이 변환만 수행한다.
        // 챌린저별 포인트는 IN 쿼리 1번으로 일괄 조회한다 -> N+1 회피.
        List<Challenger> challengers = loadChallengerPort.batchGetByMemberIdsAndGisuId(memberIds, gisuId);
        Set<Long> challengerIds = challengers.stream()
            .map(Challenger::getId)
            .collect(Collectors.toSet());
        Map<Long, List<ChallengerPointInfo>> pointsMap =
            getChallengerPointUseCase.getMapByChallengerIds(challengerIds);

        return challengers.stream()
            .collect(Collectors.toMap(
                Challenger::getMemberId,
                c -> ChallengerInfo.from(c, pointsMap.getOrDefault(c.getId(), List.of()))
            ));
    }

    @Override
    public Map<Long, ChallengerInfo> listByMemberIdsAndGisuId(Set<Long> memberIds, Long gisuId) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }
        List<Challenger> challengers = loadChallengerPort.listByMemberIdsAndGisuId(memberIds, gisuId);
        if (challengers.isEmpty()) {
            return Map.of();
        }
        Set<Long> challengerIds = challengers.stream()
            .map(Challenger::getId)
            .collect(Collectors.toSet());
        Map<Long, List<ChallengerPointInfo>> pointsMap =
            getChallengerPointUseCase.getMapByChallengerIds(challengerIds);

        return challengers.stream()
            .collect(Collectors.toMap(
                Challenger::getMemberId,
                c -> ChallengerInfo.from(c, pointsMap.getOrDefault(c.getId(), List.of()))
            ));
    }

    @Override
    public List<ChallengerInfo> getAllByGisuId(Long gisuId) {
        return toChallengerInfoListBatch(loadChallengerPort.getAllByGisuId(gisuId));
    }

    @Override
    public List<ChallengerInfo> listByChapterId(Long chapterId) {
        return loadChallengerPort.listByChapterId(chapterId).stream()
            .map(c -> ChallengerInfo.from(c, List.of()))
            .toList();
    }

    @Override
    public List<ChallengerInfo> getAllByGisuIdWithoutChallengerPoints(Long gisuId) {
        return loadChallengerPort.getAllByGisuId(gisuId).stream()
            .map(c -> ChallengerInfo.from(c, List.of()))
            .toList();
    }

    @Override
    public List<ChallengerInfo> getAllLatestGisuPerMemberWithoutChallengerPoints() {
        return loadChallengerPort.findLatestPerMember().stream()
            .map(ChallengerInfo::from)
            .toList();
    }

    private ChallengerInfo getChallengerInfoFromChallenger(Challenger challenger) {
        List<ChallengerPointInfo> challengerPointInfos =
            getChallengerPointUseCase.getListByChallengerId(challenger.getId());

        return ChallengerInfo.from(challenger, challengerPointInfos);
    }

    /**
     * 챌린저 목록의 포인트를 IN 쿼리 1번으로 일괄 조회해 ChallengerInfo 리스트로 변환합니다.
     */
    private List<ChallengerInfo> toChallengerInfoListBatch(List<Challenger> challengers) {
        if (challengers.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = challengers.stream()
            .map(Challenger::getId)
            .collect(Collectors.toSet());

        Map<Long, List<ChallengerPointInfo>> pointsMap =
            getChallengerPointUseCase.getMapByChallengerIds(ids);

        return challengers.stream()
            .map(c -> ChallengerInfo.from(c, pointsMap.getOrDefault(c.getId(), List.of())))
            .toList();
    }
}
