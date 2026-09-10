package com.umc.product.challenger.adapter.in.web.assembler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerInfoResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

/**
 * 챌린저 ID를 기반으로 회원 및 기수 정보를 포함한 응답 객체를 조립하는 헬퍼 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class ChallengerResponseAssembler {

    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChapterUseCase getChapterUseCase;

    public ChallengerInfoResponse fromChallengerId(Long challengerId) {
        ChallengerInfo challengerInfo = getChallengerUseCase.getById(challengerId);
        MemberInfo memberInfo = getMemberUseCase.getById(challengerInfo.memberId());
        GisuInfo gisuInfo = getGisuUseCase.getById(challengerInfo.gisuId());
        ChapterInfo chapterInfo = getChapterUseCase.findByGisuAndSchool(challengerInfo.gisuId(), memberInfo.schoolId())
            .orElse(null);

        return ChallengerInfoResponse.from(challengerInfo, memberInfo, gisuInfo, chapterInfo);
    }

    public List<ChallengerInfoResponse> fromMemberId(Long memberId) {
        List<ChallengerInfo> challengerInfos = getChallengerUseCase.getAllByMemberId(memberId);
        MemberInfo memberInfo = getMemberUseCase.getById(memberId);

        // 챌린저별로 gisu/chapter를 반복 조회하면 N+1이 발생하므로, batch 메서드로 한 번에 조회합니다.
        Set<Long> gisuIds = challengerInfos.stream()
            .map(ChallengerInfo::gisuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, GisuInfo> gisuByGisuId = gisuIds.isEmpty()
            ? Map.of()
            : getGisuUseCase.getByIds(gisuIds).stream()
                .collect(Collectors.toMap(GisuInfo::gisuId, g -> g));

        Map<Long, Map<Long, ChapterInfo>> chapterByGisuAndSchool = (gisuIds.isEmpty() || memberInfo.schoolId() == null)
            ? Map.of()
            : getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(gisuIds, Set.of(memberInfo.schoolId()));

        return challengerInfos.stream()
            .map(challengerInfo -> ChallengerInfoResponse.from(
                challengerInfo,
                memberInfo,
                gisuByGisuId.get(challengerInfo.gisuId()),
                getChapterInfo(chapterByGisuAndSchool, challengerInfo.gisuId(), memberInfo.schoolId())
            ))
            .toList();
    }

    private ChapterInfo getChapterInfo(
        Map<Long, Map<Long, ChapterInfo>> chapterByGisuAndSchool,
        Long gisuId,
        Long schoolId
    ) {
        if (schoolId == null) {
            return null;
        }
        return chapterByGisuAndSchool.getOrDefault(gisuId, Map.of()).get(schoolId);
    }
}
