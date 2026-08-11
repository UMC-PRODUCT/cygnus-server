package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.graphql.relay.RelayEdge;
import com.umc.product.global.graphql.relay.RelayPageInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryPageInfo;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationProgressStatus;

/**
 * 판정 이력 Connection 응답. Relay Connection 스펙 필드에 헤더 집계(asOf/progressStatus)를 추가로 담는다.
 * Spring GraphQL이 이미 구성된 Connection으로 인식하도록 클래스 이름은 {@code Connection}으로 끝난다.
 */
public record RecruitingDecisionHistoryGraphQlConnection(
    List<RelayEdge<RecruitingDecisionHistoryGraphQlResponse>> edges,
    RelayPageInfo pageInfo,
    long totalCount,
    Instant asOf,
    RecruitingEvaluationProgressStatus progressStatus
) {

    public static RecruitingDecisionHistoryGraphQlConnection from(
        RecruitingDecisionHistoryPageInfo info,
        ConnectionArguments arguments
    ) {
        RelayConnection<RecruitingDecisionHistoryGraphQlResponse> connection =
            RelayConnection.fromPage(info.page(), arguments, RecruitingDecisionHistoryGraphQlResponse::from);
        return new RecruitingDecisionHistoryGraphQlConnection(
            connection.edges(),
            connection.pageInfo(),
            connection.totalCount(),
            info.asOf(),
            info.progressStatus()
        );
    }
}
