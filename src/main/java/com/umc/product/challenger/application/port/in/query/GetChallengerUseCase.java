package com.umc.product.challenger.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfoWithStatus;

public interface GetChallengerUseCase {
    // TODO: 챌린저에 대해서 public/private 정보 구분 필요 시 method 추가해서 진행하여야 함

    /**
     * challengerId로 챌린저 정보를 조회합니다.
     */
    ChallengerInfo getById(Long challengerId);

    Optional<ChallengerInfo> findById(Long challengerId);

    /**
     * challengerId로 챌린저를 검색합니다. 존재하지 않을 경우 null을 반환합니다.
     *
     * @deprecated {@link #findById}를 사용하도록 변경해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    ChallengerInfo findByIdOrNull(Long challengerId);

    /**
     * memberId와 gisuId로 챌린저 정보 조회
     */
    ChallengerInfo getByMemberIdAndGisuId(Long memberId, Long gisuId);

    /**
     * memberId와 gisuId로 챌린저 정보 조회 (없으면 Optional.empty())
     */
    Optional<ChallengerInfo> findByMemberIdAndGisuId(Long memberId, Long gisuId);

    /**
     * memberId와 gisuId로 ACTIVE 챌린저 정보 조회
     */
    ChallengerInfo getActiveByMemberIdAndGisuId(Long memberId, Long gisuId);

    /**
     * memberId로 해당 사용자가 가지고 있는 모든 챌린저 정보 조회
     */
    List<ChallengerInfo> getAllByMemberId(Long memberId);

    /**
     * 여러 memberId로 회원별 모든 챌린저 정보를 IN 쿼리 1회로 일괄 조회합니다.
     * <p>
     * 챌린저 이력이 없는 회원은 결과 Map에 키로 등장하지 않습니다.
     */
    Map<Long, List<ChallengerInfo>> getAllByMemberIds(Set<Long> memberIds);

    /**
     * 여러 memberId로 회원별 모든 챌린저 기본 정보를 IN 쿼리 1회로 일괄 조회합니다.
     * <p>
     * 상벌점이 필요 없는 검색/목록 조립에서 사용합니다.
     */
    Map<Long, List<ChallengerBasicInfo>> getAllBasicByMemberIds(Set<Long> memberIds);

    /**
     * 특정 기수 내 여러 memberId의 챌린저 기본 정보를 조회합니다.
     * <p>
     * 해당 기수 챌린저가 없는 회원은 결과에서 제외합니다.
     */
    List<ChallengerBasicInfo> listBasicByMemberIdsAndGisuId(Set<Long> memberIds, Long gisuId);

    /**
     * 특정 기수의 챌린저 기본 정보를 상벌점 없이 조회합니다.
     * <p>
     * 활성 대상 검색처럼 챌린저 식별자·회원·파트·상태만 필요한 경우 사용합니다.
     */
    List<ChallengerBasicInfo> listBasicByGisuId(Long gisuId);

    /**
     * 여러 challengerId로 챌린저 정보 배치 조회
     *
     * @param challengerIds 챌린저 ID 목록
     * @return challengerId → ChallengerInfo Map
     * @deprecated Map을 사용하는 것이 아니라 List를 반환하도록 변경해야 합니다. 현재 사용중인 곳에서 모두 제거하게 되면 변경 예정입니다.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    Map<Long, ChallengerInfo> getAllByIdsAsMap(Set<Long> challengerIds);

    List<ChallengerInfo> getAllByIds(Set<Long> challengerIds);

    /**
     * 여러 챌린저를 IN query로 조회하며 입력된 모든 ID가 존재해야 합니다.
     */
    List<ChallengerInfo> batchGetByIds(Set<Long> challengerIds);

    /**
     * 특정 기수 내 여러 memberId 의 챌린저 정보를 batch 조회한다.
     * <p>
     * 입력된 모든 memberId 는 해당 기수의 챌린저로 존재해야 한다. 누락 시 예외.
     *
     * @return memberId -> ChallengerInfo Map
     */
    Map<Long, ChallengerInfo> batchGetByMemberIdsAndGisuId(Set<Long> memberIds, Long gisuId);

    /**
     * 특정 기수 내 여러 memberId 에 해당하는 챌린저 정보를 조회한다.
     * <p>
     * 챌린저가 없는 memberId 는 결과 Map 에 포함하지 않는다.
     *
     * @return memberId -> ChallengerInfo Map
     */
    Map<Long, ChallengerInfo> listByMemberIdsAndGisuId(Set<Long> memberIds, Long gisuId);

    /**
     * 기수 ID로 해당 기수의 모든 챌린저 정보 조회
     *
     * @param gisuId 기수 ID
     * @return 해당 기수의 챌린저 정보 목록
     */
    List<ChallengerInfo> getAllByGisuId(Long gisuId);

    /**
     * 지부 ID로 해당 지부에 속한 모든 챌린저 정보 조회 (상벌점 제외)
     * <p>
     * 지부의 기수와 일치하고, 회원의 학교가 해당 지부 소속 학교에 포함되는 챌린저를 반환합니다.
     * 모집단 집계 등 상벌점이 불필요한 경우 사용합니다.
     *
     * @param chapterId 지부 ID
     * @return 해당 지부의 챌린저 정보 목록
     */
    List<ChallengerInfo> listByChapterId(Long chapterId);

    /**
     * memberId로 해당 사용자가 가지고 있는 가장 최근 챌린저 정보 조회
     */
    ChallengerInfoWithStatus getLatestActiveChallengerByMemberId(Long memberId);

    /**
     * 기수 ID로 해당 기수의 모든 챌린저 ID 조회 (상벌점 제외)
     * <p>
     * 대상자 집계 등 상벌점이 불필요한 경우 사용합니다.
     *
     * @deprecated {@link ChallengerInfo}가 아닌 별도의 Info단 DTO를 생성해서 사용하도록 합니다. 그 전까지는 {@link #getAllByGisuId}를 사용해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    List<ChallengerInfo> getAllByGisuIdWithoutChallengerPoints(Long gisuId);

    /**
     * 각 멤버별 가장 최근 기수의 챌린저 조회 (상벌점 제외)
     * <p>
     * 대상자 집계 등 상벌점이 불필요한 경우 사용합니다.
     */
    List<ChallengerInfo> getAllLatestGisuPerMemberWithoutChallengerPoints();

}
