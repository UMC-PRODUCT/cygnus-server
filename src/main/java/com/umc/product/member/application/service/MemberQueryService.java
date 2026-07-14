package com.umc.product.member.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.member.application.port.in.query.GetMemberProfileUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberProfileInfo;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService implements GetMemberUseCase, GetMemberProfileUseCase {

    private final LoadMemberPort loadMemberPort;

    private final GetSchoolUseCase getSchoolUseCase;
    private final GetFileUseCase getFileUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    private MemberInfo toMemberInfo(Member member) {
        // 학교명 채워넣기
        String schoolName = getSchoolUseCase.getSchoolDetail(member.getSchoolId()).schoolName();

        // 프로필 이미지 링크 채워넣기
        String profileImageId = member.getProfileImageId();
        String profileImageLink =
            profileImageId == null
                ? null
                // profileImageId가 존재하는 경우 접근 가능한 링크를 반환하도록 함
                : getFileUseCase.getById(profileImageId).fileLink();

        // 역할 채워넣기
        List<ChallengerRoleInfo> roles = getChallengerRoleUseCase.findAllByMemberId(member.getId());

        return MemberInfo.from(member, schoolName, profileImageLink, roles);
    }

    @Override
    public MemberInfo getById(Long memberId) {
        return findById(memberId)
            .orElseThrow(() -> new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    public MemberInfo findByIdOrNull(Long memberId) {
        Member member = loadMemberPort.findById(memberId).orElse(null);

        return member != null
            ? toMemberInfo(member)
            : null;
    }

    @Override
    public Optional<MemberInfo> findById(Long memberId) {
        return loadMemberPort.findById(memberId)
            .map(this::toMemberInfo);
    }

    @Override
    public MemberProfileInfo getMemberProfileById(Long memberId) {
        return MemberProfileInfo.from(getOrThrowMember(memberId).getProfile());
    }

    @Override
    public Map<Long, MemberInfo> findAllByIds(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        List<Member> members = loadMemberPort.findAllByIds(memberIds);

        Set<Long> schoolIds = members.stream()
            .map(Member::getSchoolId)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> schoolNames = getSchoolUseCase.batchGetNamesByIds(schoolIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                SchoolNameInfo::schoolId,
                SchoolNameInfo::schoolName
            ));
        List<String> profileImageIds = members.stream()
            .map(Member::getProfileImageId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        Map<String, FileInfo> profileFiles = profileImageIds.isEmpty()
            ? Map.of()
            : getFileUseCase.findAllByIds(new ArrayList<>(profileImageIds));
        Map<Long, MemberInfo> results = new HashMap<>(members.size());

        for (Member member : members) {
            String schoolName = schoolNames.get(member.getSchoolId());
            FileInfo profileFile = member.getProfileImageId() == null
                ? null
                : profileFiles.get(member.getProfileImageId());
            String profileImageLink = profileFile == null ? null : profileFile.fileLink();

            results.put(member.getId(), MemberInfo.from(member, schoolName, profileImageLink, null));
        }

        return results;
    }

    @Override
    public Map<Long, MemberInfo> batchGetByIds(Set<Long> memberIds) {
        Map<Long, MemberInfo> members = findAllByIds(memberIds);
        if (memberIds != null && members.size() != memberIds.size()) {
            throw new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
        return members;
    }

    @Override
    public Map<Long, Long> findAllSchoolIdsByIds(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        return loadMemberPort.findAllByIds(memberIds).stream()
            .filter(m -> m.getSchoolId() != null)
            .collect(java.util.stream.Collectors.toMap(Member::getId, Member::getSchoolId));
    }

    @Override
    public Set<Long> listIdsBySchoolId(Long schoolId) {
        if (schoolId == null) {
            return Set.of();
        }
        return loadMemberPort.listIdsBySchoolId(schoolId);
    }

    @Override
    public Map<Long, Set<Long>> listIdsBySchoolIds(Set<Long> schoolIds) {
        if (schoolIds == null || schoolIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<Long>> result = new HashMap<>(loadMemberPort.listIdsBySchoolIds(schoolIds));
        for (Long schoolId : schoolIds) {
            result.putIfAbsent(schoolId, Set.of());
        }
        return result;
    }

    @Override
    public boolean existsById(Long memberId) {
        return loadMemberPort.existsById(memberId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return loadMemberPort.existsByEmail(email);
    }

    @Override
    public List<Long> findAllIdsCursor(Long lastId, int limit) {
        return loadMemberPort.findAllIdsCursor(lastId, PageRequest.of(0, limit));
    }

    @Override
    public long countMembersByIds(Set<Long> ids) {
        return loadMemberPort.countMembersByIds(ids);
    }

    @Override
    public long countAll() {
        return loadMemberPort.countAllMembers();
    }

    // === Private Methods ===

    private Member getOrThrowMember(Long memberId) {
        return loadMemberPort.findById(memberId)
            .orElseThrow(() -> new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
