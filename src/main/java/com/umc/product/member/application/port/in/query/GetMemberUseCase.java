package com.umc.product.member.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.member.application.port.in.query.dto.MemberInfo;

public interface GetMemberUseCase {
    MemberInfo getById(Long memberId);

    Optional<MemberInfo> findById(Long memberId);

    /**
     * 멤버 ID로 멤버 정보를 조회합니다. 멤버가 존재하지 않을 경우 null을 반환합니다.
     *
     * @deprecated {@link #findById}를 사용해주세요.
     */
    @Deprecated(since = "v1.5.0", forRemoval = true)
    MemberInfo findByIdOrNull(Long memberId);

    Map<Long, MemberInfo> findAllByIds(Set<Long> memberIds);

    Map<Long, String> findAllNamesByIds(Set<Long> memberIds);

    Set<Long> searchIdsByName(Set<Long> candidateMemberIds, String name);

    Map<Long, Long> findAllSchoolIdsByIds(Set<Long> memberIds);

    /**
     * 특정 학교에 소속된 모든 멤버의 ID 집합을 반환한다.
     *
     * @param schoolId 학교 ID
     * @return 해당 학교 소속 멤버의 ID 집합 (없으면 빈 집합)
     */
    Set<Long> listIdsBySchoolId(Long schoolId);

    /**
     * 여러 학교에 소속된 멤버 ID 집합을 학교별로 반환한다.
     *
     * @param schoolIds 학교 ID 집합
     * @return schoolId -> memberId 집합 Map (멤버가 없는 학교는 빈 집합)
     */
    Map<Long, Set<Long>> listIdsBySchoolIds(Set<Long> schoolIds);

    boolean existsById(Long memberId);

    boolean existsByEmail(String email);

    /**
     * 커서 기반으로 멤버 ID 목록을 조회합니다. lastId보다 큰 ID를 가진 멤버를 최대 limit개 반환합니다. 전체 멤버를 메모리 효율적으로 순회할 때 사용합니다.
     */
    List<Long> findAllIdsCursor(Long lastId, int limit);

    /**
     * @param ids member의 id 목록의 Set
     * @return ids 중에 실제로 존재하는 member의 개수
     */
    long countMembersByIds(Set<Long> ids);

    /**
     * 전체 멤버 수를 반환합니다. 시딩 시퀀스 오프셋 산정 등 운영 외 용도에 사용합니다.
     */
    long countAll();
}
