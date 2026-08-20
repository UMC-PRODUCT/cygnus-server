package com.umc.product.demoday.application.service.query;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.query.ListDemodayAdminVoteUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteInfo.ParticipantInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteListInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.dto.ListDemodayAdminVoteQuery;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.out.DemodayVoteSearchCondition;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SearchDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.enums.DemodayVoteStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemodayAdminVoteQueryService implements ListDemodayAdminVoteUseCase {

    private static final String UNKNOWN_MEMBER_NAME = "알 수 없는 회원";
    private static final String UNKNOWN_BOOTH_NAME = "알 수 없는 부스";

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final LoadDemodayPollPort loadDemodayPollPort;
    private final SearchDemodayVotePort searchDemodayVotePort;
    private final LoadDemodayBoothPort loadDemodayBoothPort;
    private final LoadDemodayEntryCodePort loadDemodayEntryCodePort;
    private final GetMemberUseCase getMemberUseCase;
    private final GetProjectUseCase getProjectUseCase;

    @Override
    public DemodayAdminVoteListInfo listVotes(ListDemodayAdminVoteQuery query) {
        DemodayPoll poll = loadDemodayPollPort.findById(query.pollId())
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        adminAccessChecker.validateAdminAccess(query.requesterMemberId(), poll.getGisuId());

        Set<Long> matchingMemberIds = resolveMatchingMemberIds(query);
        if (query.hasParticipantNameFilter() && matchingMemberIds.isEmpty()) {
            DemodayAdminVoteListInfo emptyResult = DemodayAdminVoteListInfo.empty(query.pollId());
            logVoteListAccess(query, 0);
            return emptyResult;
        }

        List<DemodayVote> fetched = searchDemodayVotePort.search(new DemodayVoteSearchCondition(
            query.pollId(),
            query.cursor(),
            query.boothId(),
            matchingMemberIds,
            query.size() + 1
        ));

        boolean hasNext = fetched.size() > query.size();
        List<DemodayVote> page = hasNext ? fetched.subList(0, query.size()) : fetched;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;

        DemodayAdminVoteListInfo result = new DemodayAdminVoteListInfo(
            query.pollId(),
            assembleVotes(query.pollId(), page),
            nextCursor,
            hasNext
        );
        logVoteListAccess(query, result.content().size());
        return result;
    }

    private Set<Long> resolveMatchingMemberIds(ListDemodayAdminVoteQuery query) {
        if (!query.hasParticipantNameFilter()) {
            return null;
        }

        Set<Long> candidateMemberIds = searchDemodayVotePort.listMemberIds(query.pollId(), query.boothId());
        return getMemberUseCase.searchIdsByName(candidateMemberIds, query.participantName());
    }

    private List<DemodayAdminVoteInfo> assembleVotes(Long pollId, List<DemodayVote> votes) {
        if (votes.isEmpty()) {
            return List.of();
        }

        Map<Long, DemodayBooth> boothsById = loadDemodayBoothPort.listByPollId(pollId).stream()
            .collect(Collectors.toMap(DemodayBooth::getId, Function.identity()));
        Map<Long, ProjectInfo> projectsById = loadProjects(votes, boothsById);
        Map<Long, String> memberNamesById = getMemberUseCase.findAllNamesByIds(memberIdsOf(votes));
        Map<Long, Integer> guestOrdinalsByEntryCodeId = guestOrdinals(pollId, votes);

        return votes.stream()
            .map(vote -> toInfo(
                vote,
                boothsById,
                projectsById,
                memberNamesById,
                guestOrdinalsByEntryCodeId
            ))
            .toList();
    }

    private Map<Long, ProjectInfo> loadProjects(
        List<DemodayVote> votes,
        Map<Long, DemodayBooth> boothsById
    ) {
        Set<Long> projectIds = votes.stream()
            .map(DemodayVote::getTargetBoothId)
            .map(boothsById::get)
            .filter(Objects::nonNull)
            .map(DemodayBooth::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return projectIds.isEmpty() ? Map.of() : getProjectUseCase.findAllByIds(projectIds);
    }

    private Set<Long> memberIdsOf(List<DemodayVote> votes) {
        return votes.stream()
            .map(DemodayVote::getMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Map<Long, Integer> guestOrdinals(Long pollId, List<DemodayVote> votes) {
        boolean containsGuestVote = votes.stream().anyMatch(vote -> vote.getEntryCodeId() != null);
        if (!containsGuestVote) {
            return Map.of();
        }

        List<DemodayEntryCode> redeemedEntryCodes = loadDemodayEntryCodePort.listRedeemedByPollId(pollId);
        Map<Long, Integer> ordinals = new LinkedHashMap<>(redeemedEntryCodes.size());
        for (int index = 0; index < redeemedEntryCodes.size(); index++) {
            ordinals.put(redeemedEntryCodes.get(index).getId(), index + 1);
        }
        return ordinals;
    }

    private DemodayAdminVoteInfo toInfo(
        DemodayVote vote,
        Map<Long, DemodayBooth> boothsById,
        Map<Long, ProjectInfo> projectsById,
        Map<Long, String> memberNamesById,
        Map<Long, Integer> guestOrdinalsByEntryCodeId
    ) {
        DemodayBooth booth = Objects.requireNonNull(
            boothsById.get(vote.getTargetBoothId()),
            "투표 대상 부스를 찾을 수 없습니다: " + vote.getTargetBoothId()
        );

        return new DemodayAdminVoteInfo(
            vote.getId(),
            vote.getCreatedAt(),
            participantOf(vote, memberNamesById, guestOrdinalsByEntryCodeId),
            new DemodayBoothInfo(
                booth.getId(),
                booth.getProjectId(),
                resolveDisplayName(booth, projectsById)
            ),
            vote.isRevoked() ? DemodayVoteStatus.REVOKED : DemodayVoteStatus.VALID,
            vote.getRevokedAt()
        );
    }

    private ParticipantInfo participantOf(
        DemodayVote vote,
        Map<Long, String> memberNamesById,
        Map<Long, Integer> guestOrdinalsByEntryCodeId
    ) {
        if (vote.getMemberId() != null) {
            return new ParticipantInfo(
                DemodayParticipantType.MEMBER,
                memberNamesById.getOrDefault(vote.getMemberId(), UNKNOWN_MEMBER_NAME)
            );
        }

        Integer ordinal = Objects.requireNonNull(
            guestOrdinalsByEntryCodeId.get(vote.getEntryCodeId()),
            "외부 방문자 입장 순번을 찾을 수 없습니다: " + vote.getEntryCodeId()
        );
        return new ParticipantInfo(DemodayParticipantType.GUEST, "외부인 " + ordinal + "번");
    }

    private String resolveDisplayName(DemodayBooth booth, Map<Long, ProjectInfo> projectsById) {
        if (booth.getProjectId() == null) {
            return booth.getDisplayName();
        }

        ProjectInfo project = projectsById.get(booth.getProjectId());
        if (project != null && project.name() != null && !project.name().isBlank()) {
            return project.name();
        }
        if (booth.getDisplayName() != null && !booth.getDisplayName().isBlank()) {
            return booth.getDisplayName();
        }
        return UNKNOWN_BOOTH_NAME;
    }

    private void logVoteListAccess(ListDemodayAdminVoteQuery query, int resultCount) {
        log.info(
            "demoday_admin_votes_viewed",
            kv("actorMemberId", query.requesterMemberId()),
            kv("pollId", query.pollId()),
            kv("boothId", query.boothId()),
            kv("cursor", query.cursor()),
            kv("size", query.size()),
            kv("participantNameFilterApplied", query.hasParticipantNameFilter()),
            kv("resultCount", resultCount)
        );
    }
}
