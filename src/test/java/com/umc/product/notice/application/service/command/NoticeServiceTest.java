package com.umc.product.notice.application.service.command;

import static com.umc.product.support.fixture.NoticeUnitFixture.challengerTarget;
import static com.umc.product.support.fixture.NoticeUnitFixture.notice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.application.port.in.command.ManageNoticeContentUseCase;
import com.umc.product.notice.application.port.in.command.dto.CreateNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.DeleteNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.SendNoticeReminderCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeCommand;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.ManageNoticeTargetPort;
import com.umc.product.notice.application.port.out.SaveNoticePort;
import com.umc.product.notice.application.port.out.SaveNoticeReadPort;
import com.umc.product.notice.application.port.out.SaveNoticeTargetPort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notification.application.port.in.RequestFcmNotificationUseCase;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notice command service 테스트")
class NoticeServiceTest {

    @Mock
    LoadNoticePort loadNoticePort;

    @Mock
    SaveNoticePort saveNoticePort;

    @Mock
    SaveNoticeTargetPort saveNoticeTargetPort;

    @Mock
    ManageNoticeTargetPort manageNoticeTargetPort;

    @Mock
    SaveNoticeReadPort saveNoticeReadPort;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    ManageNoticeContentUseCase manageNoticeContentUseCase;

    @Mock
    RequestFcmNotificationUseCase requestFcmNotificationUseCase;

    NoticeService service;

    @BeforeEach
    void setUp() {
        service = new NoticeService(
            loadNoticePort,
            saveNoticePort,
            saveNoticeTargetPort,
            manageNoticeTargetPort,
            saveNoticeReadPort,
            getChallengerRoleUseCase,
            getChallengerUseCase,
            manageNoticeContentUseCase,
            requestFcmNotificationUseCase
        );
    }

    @Test
    @DisplayName("빈 bulk는 저장하지 않고 정상 bulk는 ID와 target을 순서대로 생성한다")
    void bulk_생성은_빈_입력과_정상_입력을_처리한다() {
        assertThat(service.createNoticeBulk(List.of())).isEmpty();
        verify(saveNoticePort, never()).save(any());

        CreateNoticeCommand command = command(false, challengerTarget());
        given(getChallengerRoleUseCase.isCentralMemberInGisu(10L, 1L)).willReturn(true);
        given(saveNoticePort.save(any())).willAnswer(invocation -> {
            Notice saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        assertThat(service.createNoticeBulk(List.of(command))).containsExactly(100L);
        verify(saveNoticeTargetPort).save(any());
        verify(requestFcmNotificationUseCase, never()).request(any());
    }

    @Test
    @DisplayName("알림 공지는 대상과 축약 제목·본문을 FCM에 전달하고 재전송을 방지한다")
    void 알림_공지는_FCM을_요청하고_notified로_전환한다() {
        NoticeTargetInfo targetInfo = new NoticeTargetInfo(1L, 2L, null, null, NoticeTab.CHALLENGER);
        CreateNoticeCommand command = new CreateNoticeCommand(
            10L, "아주 긴 공지 제목 ".repeat(3), "아주 긴 공지 내용 ".repeat(5), true, true, targetInfo
        );
        given(getChallengerRoleUseCase.isChapterPresidentInGisu(10L, 1L, 2L)).willReturn(true);
        given(saveNoticePort.save(any())).willAnswer(invocation -> {
            Notice saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        assertThat(service.createNotice(command)).isEqualTo(100L);

        ArgumentCaptor<RequestFcmNotificationCommand> captor =
            ArgumentCaptor.forClass(RequestFcmNotificationCommand.class);
        verify(requestFcmNotificationUseCase).request(captor.capture());
        assertThat(captor.getValue().targetGisuId()).isEqualTo(1L);
        assertThat(captor.getValue().targetChapterId()).isEqualTo(2L);
        assertThat(captor.getValue().targetSchoolId()).isNull();
        assertThat(captor.getValue().targetParts()).isEmpty();
        assertThat(captor.getValue().title()).hasSizeLessThanOrEqualTo(25);
        assertThat(captor.getValue().body()).hasSizeLessThanOrEqualTo(40);
    }

    @Test
    @DisplayName("파트 대상 알림은 입력 목록을 중복 없는 FCM 대상 Set으로 변환한다")
    void 파트_대상_알림은_FCM_Set으로_변환한다() {
        NoticeTargetInfo targetInfo = new NoticeTargetInfo(
            1L, null, null,
            List.of(ChallengerPart.SPRINGBOOT, ChallengerPart.SPRINGBOOT, ChallengerPart.WEB),
            NoticeTab.CHALLENGER
        );
        CreateNoticeCommand command = command(true, targetInfo);
        given(getChallengerRoleUseCase.isCentralMemberInGisu(10L, 1L)).willReturn(true);
        given(saveNoticePort.save(any())).willAnswer(invocation -> {
            Notice saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        service.createNotice(command);

        ArgumentCaptor<RequestFcmNotificationCommand> captor =
            ArgumentCaptor.forClass(RequestFcmNotificationCommand.class);
        verify(requestFcmNotificationUseCase).request(captor.capture());
        assertThat(captor.getValue().targetParts())
            .containsExactlyInAnyOrder(ChallengerPart.SPRINGBOOT, ChallengerPart.WEB);
    }

    @Test
    @DisplayName("일반 권한이 없으면 SUPER_ADMIN만 생성할 수 있다")
    void 권한_부족은_SUPER_ADMIN으로만_override한다() {
        CreateNoticeCommand command = command(false, challengerTarget());
        given(getChallengerRoleUseCase.isCentralMemberInGisu(10L, 1L)).willReturn(false);
        given(getChallengerRoleUseCase.isSuperAdmin(10L)).willReturn(false, true);

        assertThatThrownBy(() -> service.createNotice(command)).isInstanceOf(NoticeDomainException.class);

        given(saveNoticePort.save(any())).willAnswer(invocation -> {
            Notice saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        assertThat(service.createNotice(command)).isEqualTo(100L);
    }

    @Test
    @DisplayName("수정은 제목·내용·필독을 변경하고 미존재 공지를 거부한다")
    void 수정을_적용하고_미존재를_거부한다() {
        Notice target = notice();
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(target));
        service.updateNoticeTitleOrContent(new UpdateNoticeCommand(10L, 1L, "수정", "수정 내용", true));
        assertThat(target.getTitle()).isEqualTo("수정");
        assertThat(target.isMustRead()).isTrue();

        given(loadNoticePort.findNoticeById(2L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateNoticeTitleOrContent(
            new UpdateNoticeCommand(10L, 2L, "수정", "수정 내용", true)
        )).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("삭제는 부가 콘텐츠, 읽음, 대상, 공지 순으로 정리한다")
    void 삭제는_모든_연관_데이터를_정리한다() {
        Notice target = notice();
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(target));

        service.deleteNotice(new DeleteNoticeCommand(10L, 1L));

        verify(manageNoticeContentUseCase).removeContentsByNoticeId(1L, 10L);
        verify(saveNoticeReadPort).deleteAllByNoticeId(1L);
        verify(manageNoticeTargetPort).deleteByNoticeId(1L);
        verify(saveNoticePort).delete(target);
    }

    @Test
    @DisplayName("리마인드는 중복 challenger ID를 제거하고 조회된 member ID로만 발송한다")
    void 리마인드는_challenger를_member로_변환한다() {
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
        ChallengerInfo first = ChallengerInfo.builder().challengerId(2L).memberId(20L).build();
        ChallengerInfo second = ChallengerInfo.builder().challengerId(3L).memberId(30L).build();
        given(getChallengerUseCase.getAllByIds(Set.of(2L, 3L))).willReturn(List.of(first, second));

        service.remindNotice(new SendNoticeReminderCommand(10L, 1L, List.of(2L, 2L, 3L)));

        ArgumentCaptor<RequestFcmNotificationCommand> captor =
            ArgumentCaptor.forClass(RequestFcmNotificationCommand.class);
        verify(requestFcmNotificationUseCase).request(captor.capture());
        assertThat(captor.getValue().memberIds()).containsExactly(20L, 30L);
        assertThat(captor.getValue().title()).startsWith("[⏰ 공지사항 리마인드]");
    }

    @Test
    @DisplayName("조회수 증가는 persistence port에 그대로 위임한다")
    void 조회수를_증가시킨다() {
        service.incrementViewCount(1L);
        verify(saveNoticePort).incrementViewCount(1L);
    }

    private static CreateNoticeCommand command(boolean notify, NoticeTargetInfo targetInfo) {
        return new CreateNoticeCommand(10L, "공지 제목", "공지 내용", notify, false, targetInfo);
    }
}
