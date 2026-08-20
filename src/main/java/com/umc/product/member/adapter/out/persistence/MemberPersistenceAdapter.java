package com.umc.product.member.adapter.out.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.member.application.dto.MemberSearchAccessScope;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.application.port.out.SearchMemberInvitationPort;
import com.umc.product.member.application.port.out.SearchMemberPort;
import com.umc.product.member.application.port.out.dto.MemberInvitationCandidatePage;
import com.umc.product.member.application.port.out.dto.SearchMemberInvitationCondition;
import com.umc.product.member.domain.Member;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements
    LoadMemberPort,
    SaveMemberPort,
    SearchMemberPort,
    SearchMemberInvitationPort {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberQueryRepository memberQueryRepository;

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id);
    }

    @Override
    public Optional<Member> findByIdForUpdate(Long id) {
        return memberQueryRepository.findByIdWithPessimisticLock(id);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return memberJpaRepository.findByEmail(email);
    }

    // QueryDSL 사용 테스트 하고자 넣은 method
    @Override
    public Optional<Member> findByNickname(String nickname) {
        return memberQueryRepository.findByNickname(nickname);
    }

    @Override
    public List<Member> findAllByIds(Set<Long> ids) {
        return memberJpaRepository.findAllById(ids)
            .stream().toList();
    }

    @Override
    public Set<Long> searchIdsByName(Set<Long> candidateMemberIds, String name) {
        return memberJpaRepository.searchIdsByName(candidateMemberIds, name);
    }

    @Override
    public Set<Long> listIdsBySchoolId(Long schoolId) {
        return memberJpaRepository.findAllIdsBySchoolId(schoolId);
    }

    @Override
    public Map<Long, Set<Long>> listIdsBySchoolIds(Set<Long> schoolIds) {
        if (schoolIds == null || schoolIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<Long>> result = new HashMap<>();
        for (MemberJpaRepository.SchoolMemberIdRow row : memberJpaRepository.findAllIdsBySchoolIds(schoolIds)) {
            result.computeIfAbsent(row.getSchoolId(), ignored -> new HashSet<>()).add(row.getMemberId());
        }
        return result;
    }

    @Override
    public boolean existsById(Long id) {
        return memberJpaRepository.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return memberJpaRepository.existsByNickname(nickname);
    }

    @Override
    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }

    @Override
    public List<Member> saveAll(List<Member> members) {
        return memberJpaRepository.saveAll(members);
    }

    @Override
    public void delete(Member member) {
        memberJpaRepository.delete(member);
    }

    @Override
    public Page<Challenger> search(SearchMemberQuery query, Pageable pageable) {
        return memberQueryRepository.searchBy(query, pageable);
    }

    @Override
    public Page<Long> searchMemberIds(SearchMemberQuery query, Pageable pageable) {
        return memberQueryRepository.searchMemberIdsBy(query, pageable);
    }

    @Override
    public Page<Long> searchMemberIds(SearchMemberQuery query, MemberSearchAccessScope scope, Pageable pageable) {
        return memberQueryRepository.searchMemberIdsBy(query, scope, pageable);
    }

    @Override
    public MemberInvitationCandidatePage search(SearchMemberInvitationCondition condition) {
        return memberQueryRepository.searchInvitationCandidates(condition);
    }

    @Override
    public Set<Long> findActiveMemberIds(Set<Long> memberIds) {
        return memberQueryRepository.findActiveMemberIds(memberIds);
    }

    @Override
    public List<Long> findAllIdsCursor(Long lastId, Pageable pageable) {
        return memberJpaRepository.findIdsCursor(lastId, pageable);
    }

    @Override
    public long countMembersByIds(Set<Long> memberIds) {

        if (memberIds == null || memberIds.isEmpty()) {
            return 0L;
        }
        return memberJpaRepository.countByIdIn(memberIds);
    }

    @Override
    public long countAllMembers() {
        return memberJpaRepository.count();
    }
}
