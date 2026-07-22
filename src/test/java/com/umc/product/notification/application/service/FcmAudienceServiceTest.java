package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notification.application.port.in.dto.AudienceNotificationCommand;
import com.umc.product.notification.application.port.in.dto.NotificationCommand;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.application.port.out.SendFcmMessagePort;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.domain.FcmToken;
import com.umc.product.notification.domain.exception.FcmDomainException;
import com.umc.product.notification.domain.exception.FcmErrorCode;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmAudienceService")
class FcmAudienceServiceTest {

    @Mock
    SendFcmMessagePort sendFcmMessagePort;
    @Mock
    LoadFcmPort loadFcmPort;
    @Mock
    SaveFcmPort saveFcmPort;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    OperationalMetrics operationalMetrics;

    FcmAudienceService enabled;

    @BeforeEach
    void setUp() {
        enabled = service(true);
    }

    @Test
    @DisplayName("FCM 비활성화 시 audience·단일·복수 발송을 모두 no-op 처리한다")
    void disabled_is_noop_for_all_entry_points() {
        FcmAudienceService disabled = service(false);

        disabled.sendToAudience(new AudienceNotificationCommand(null, "제목", "본문"));
        disabled.sendToMember(new NotificationCommand(1L, "제목", "본문"));
        disabled.sendToMembers(List.of(1L), "제목", "본문");

        verifyNoInteractions(sendFcmMessagePort, loadFcmPort, saveFcmPort);
    }

    @Test
    @DisplayName("대상·챌린저·활성 토큰이 없으면 외부 발송을 단축한다")
    void empty_audience_and_tokens_short_circuit() {
        enabled.sendToAudience(new AudienceNotificationCommand(null, "제목", "본문"));

        NoticeTargetInfo target = target(1L, null, null, Set.of());
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(List.of());
        enabled.sendToAudience(new AudienceNotificationCommand(target, "제목", "본문"));

        ChallengerInfo challenger = challenger(1L, 10L, 1L, ChallengerPart.WEB);
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(List.of(challenger));
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(10L))).willReturn(Map.of(10L, 20L));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(1L), Set.of(20L)))
            .willReturn(Map.of(1L, Map.of(20L, new ChapterInfo(30L, "지부"))));
        given(loadFcmPort.listActiveByMemberIds(List.of(10L))).willReturn(List.of());
        enabled.sendToAudience(new AudienceNotificationCommand(target, "제목", "본문"));

        given(loadFcmPort.listActiveByMemberId(10L)).willReturn(List.of());
        enabled.sendToMember(new NotificationCommand(10L, "제목", "본문"));
        enabled.sendToMembers(null, "제목", "본문");
        enabled.sendToMembers(List.of(), "제목", "본문");
        given(loadFcmPort.listActiveByMemberIds(List.of(99L))).willReturn(List.of());
        enabled.sendToMembers(List.of(99L), "제목", "본문");

        verify(sendFcmMessagePort, never()).send(any());
    }

    @Test
    @DisplayName("audience는 학교·지부·파트를 일괄 해석하고 invalid token만 비활성화한다")
    void audience_filters_targets_and_deactivates_invalid_tokens() {
        List<ChallengerInfo> challengers = List.of(
            challenger(1L, 10L, 1L, ChallengerPart.WEB),
            challenger(2L, 11L, 1L, ChallengerPart.WEB),
            challenger(3L, 12L, 1L, ChallengerPart.SPRINGBOOT)
        );
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(challengers);
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(10L, 11L, 12L)))
            .willReturn(Map.of(10L, 20L, 12L, 21L));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(1L), Set.of(20L, 21L)))
            .willReturn(Map.of(1L, Map.of(20L, new ChapterInfo(30L, "지부"))));
        FcmToken first = token(100L, 10L);
        FcmToken second = token(101L, 10L);
        given(loadFcmPort.listActiveByMemberIds(List.of(10L))).willReturn(List.of(first, second));
        given(sendFcmMessagePort.send(any())).willReturn(FcmSendResult.of(1, 1, List.of(100L), List.of()));

        enabled.sendToAudience(new AudienceNotificationCommand(
            target(1L, 30L, 20L, Set.of(ChallengerPart.WEB)), "제목", "본문"
        ));

        assertThat(first.isActive()).isFalse();
        assertThat(second.isActive()).isTrue();
        verify(saveFcmPort).save(first);
        verify(operationalMetrics).recordNotification("FCM", "SEND_TO_AUDIENCE", "success", 1);
        verify(operationalMetrics).recordNotification("FCM", "SEND_TO_AUDIENCE", "failure", 1);
    }

    @Test
    @DisplayName("단일·복수 발송은 500개씩 분할하고 배치 예외를 부분 실패로 집계한다")
    void member_sends_partition_and_count_partial_failures() {
        FcmToken single = token(1L, 1L);
        given(loadFcmPort.listActiveByMemberId(1L)).willReturn(List.of(single));
        given(sendFcmMessagePort.send(any())).willReturn(FcmSendResult.of(1, 0, null, null));
        enabled.sendToMember(new NotificationCommand(1L, "제목", "본문"));
        verify(operationalMetrics).recordNotification("FCM", "SEND_TO_MEMBER", "success", 1);

        List<FcmToken> tokens = new ArrayList<>();
        for (long id = 1; id <= 501; id++) {
            tokens.add(token(id + 1000, id));
        }
        List<Long> memberIds = tokens.stream().map(FcmToken::getMemberId).toList();
        given(loadFcmPort.listActiveByMemberIds(memberIds)).willReturn(tokens);
        given(sendFcmMessagePort.send(any()))
            .willReturn(FcmSendResult.of(500, 0, List.of(), List.of()))
            .willThrow(new FcmDomainException(FcmErrorCode.FCM_SEND_FAILED));

        enabled.sendToMembers(memberIds, "대량", "본문");

        ArgumentCaptor<FcmSendRequest> captor = ArgumentCaptor.forClass(FcmSendRequest.class);
        verify(sendFcmMessagePort, org.mockito.Mockito.times(3)).send(captor.capture());
        assertThat(captor.getAllValues().get(1).targets()).hasSize(500);
        assertThat(captor.getAllValues().get(2).targets()).hasSize(1);
        verify(operationalMetrics).recordNotification("FCM", "SEND_TO_MEMBERS", "success", 500);
        verify(operationalMetrics).recordNotification("FCM", "SEND_TO_MEMBERS", "failure", 1);
    }

    private FcmAudienceService service(boolean enabled) {
        return new FcmAudienceService(
            new FcmProperties(enabled, true), sendFcmMessagePort, loadFcmPort, saveFcmPort,
            getChallengerUseCase, getMemberUseCase, getChapterUseCase, operationalMetrics
        );
    }

    private NoticeTargetInfo target(Long gisuId, Long chapterId, Long schoolId, Set<ChallengerPart> parts) {
        return new NoticeTargetInfo(gisuId, chapterId, schoolId, List.copyOf(parts), NoticeTab.CHALLENGER);
    }

    private ChallengerInfo challenger(Long id, Long memberId, Long gisuId, ChallengerPart part) {
        return ChallengerInfo.builder().challengerId(id).memberId(memberId).gisuId(gisuId).part(part).build();
    }

    private FcmToken token(Long id, Long memberId) {
        FcmToken token = FcmToken.create(memberId, "installation-" + id, "token-" + id);
        ReflectionTestUtils.setField(token, "id", id);
        return token;
    }
}
