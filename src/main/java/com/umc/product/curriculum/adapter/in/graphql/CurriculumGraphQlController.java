package com.umc.product.curriculum.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.challenger.adapter.in.graphql.dto.ChallengerGraphQlResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.adapter.in.graphql.dto.CurriculumGraphQlResponse;
import com.umc.product.curriculum.application.port.in.query.GetChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.GetCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.query.GetOriginalWorkbookUseCase;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CurriculumGraphQlController {

    private final GetCurriculumUseCase getCurriculumUseCase;
    private final GetOriginalWorkbookUseCase getOriginalWorkbookUseCase;
    private final GetChallengerWorkbookUseCase getChallengerWorkbookUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;

    @QueryMapping
    public CurriculumGraphQlResponse curriculum(
        @Argument Long gisuId,
        @Argument ChallengerPart part,
        @Argument Long weekNo
    ) {
        return CurriculumGraphQlResponse.from(
            getCurriculumUseCase.getCurriculumOverview(gisuId, part, weekNo)
        );
    }

    @QueryMapping
    public CurriculumGraphQlResponse myCurriculum(
        @CurrentMember MemberPrincipal principal,
        @Argument Long gisuId
    ) {
        return CurriculumGraphQlResponse.from(
            getCurriculumUseCase.getMyProgress(principal.getMemberId(), gisuId)
        );
    }

    @QueryMapping
    public CurriculumGraphQlResponse.Workbook originalWorkbook(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        return CurriculumGraphQlResponse.Workbook.from(getOriginalWorkbookUseCase.getById(id));
    }

    @QueryMapping
    public CurriculumGraphQlResponse.ChallengerWorkbook challengerWorkbook(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        return CurriculumGraphQlResponse.ChallengerWorkbook.from(getChallengerWorkbookUseCase.getById(id));
    }

    @SchemaMapping(typeName = "OriginalWorkbook", field = "challengerWorkbook")
    public CurriculumGraphQlResponse.ChallengerWorkbook challengerWorkbook(
        CurriculumGraphQlResponse.Workbook workbook
    ) {
        return workbook.challengerWorkbookId() == null
            ? null
            : CurriculumGraphQlResponse.ChallengerWorkbook.from(
                getChallengerWorkbookUseCase.getById(workbook.challengerWorkbookId())
            );
    }

    @SchemaMapping(typeName = "ChallengerWorkbook", field = "originalWorkbook")
    public CurriculumGraphQlResponse.Workbook originalWorkbook(
        CurriculumGraphQlResponse.ChallengerWorkbook workbook
    ) {
        return CurriculumGraphQlResponse.Workbook.from(
            getOriginalWorkbookUseCase.getById(workbook.originalWorkbookId())
        );
    }

    @BatchMapping(typeName = "MissionFeedback", field = "reviewer")
    public Map<CurriculumGraphQlResponse.Feedback, MemberPublicInfo> reviewers(
        List<CurriculumGraphQlResponse.Feedback> feedbacks
    ) {
        Set<Long> memberIds = feedbacks.stream()
            .map(CurriculumGraphQlResponse.Feedback::reviewerMemberId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<CurriculumGraphQlResponse.Feedback, MemberPublicInfo> result = new LinkedHashMap<>();
        for (CurriculumGraphQlResponse.Feedback feedback : feedbacks) {
            result.put(feedback, membersById.get(feedback.reviewerMemberId()));
        }
        return result;
    }

    @BatchMapping(typeName = "OriginalWorkbook", field = "releasedMember")
    public Map<CurriculumGraphQlResponse.Workbook, MemberPublicInfo> releasedMembers(
        List<CurriculumGraphQlResponse.Workbook> workbooks
    ) {
        Set<Long> memberIds = workbooks.stream()
            .map(CurriculumGraphQlResponse.Workbook::releasedMemberId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<CurriculumGraphQlResponse.Workbook, MemberPublicInfo> result = new LinkedHashMap<>();
        for (CurriculumGraphQlResponse.Workbook workbook : workbooks) {
            result.put(workbook, membersById.get(workbook.releasedMemberId()));
        }
        return result;
    }

    @BatchMapping(typeName = "ChallengerWorkbook", field = "challenger")
    public Map<CurriculumGraphQlResponse.ChallengerWorkbook, ChallengerGraphQlResponse> challengers(
        List<CurriculumGraphQlResponse.ChallengerWorkbook> workbooks
    ) {
        Set<Long> challengerIds = workbooks.stream()
            .map(CurriculumGraphQlResponse.ChallengerWorkbook::challengerId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ChallengerInfo> challengersById = getChallengerUseCase.getAllByIds(challengerIds).stream()
            .collect(Collectors.toMap(ChallengerInfo::challengerId, java.util.function.Function.identity()));
        Map<CurriculumGraphQlResponse.ChallengerWorkbook, ChallengerGraphQlResponse> result = new LinkedHashMap<>();
        for (CurriculumGraphQlResponse.ChallengerWorkbook workbook : workbooks) {
            result.put(workbook, ChallengerGraphQlResponse.from(challengersById.get(workbook.challengerId())));
        }
        return result;
    }
}
