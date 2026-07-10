package com.umc.product.global.websocket.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

/**
 * 브로커 경로 보호를 위한 공통 STOMP 인터셉터.
 *
 * <p><b>현재 책임(공통):</b> 클라이언트가 broker destination({@code /topic}, {@code /queue})으로
 * <b>직접 SEND</b> 하는 것을 차단한다. broadcast 는 서버만 수행하며 클라이언트의 broker 직접 발행은 허용하지 않는다.
 *
 * <p><b>chat 토픽 SUBSCRIBE — fail-closed 전면 차단:</b><br>
 * {@code /topic/**} 구독은 simple broker 가 직접 처리하므로, "이 사용자가 이 방을 구독할 수 있는가" 같은 리소스 단위 SUBSCRIBE 인가를 걸 지점이 없다. 아직 그 인가를
 * 소유할 소비 도메인이 붙지 않았으므로, chat 엔진 토픽({@code /topic/chat/**})의 SUBSCRIBE 는 <b>전면 거부(fail-closed)</b> 한다. 이렇게 하면 참여하지 않은 방의
 * 메시지가 새어나가는 경로 자체가 닫힌다.
 * <ul>
 *   <li>실제 접근 규칙(누가 어떤 방을 볼 수 있는가)은 도메인마다 다르므로 <b>소비 도메인</b>이 소유한다.
 *       chat 엔진은 기능만 UseCase 로 제공할 뿐 구독 인가를 책임지지 않는다.</li>
 *   <li>이 전면 거부는 임시 placeholder 다. 그 규칙을 SUBSCRIBE 프레임에 걸어주는 <b>공통 틀</b>
 *       (REST 의 {@code @CheckAccess} 에 대응하는 WS 구독 인가)을 도메인 공통 인프라로 한 번만 두고,
 *       실제 소비 도메인(inquiry)이 붙는 시점에 방별 authorizer 로 이 전면 거부를 대체한다.</li>
 * </ul>
 * chat 외의 {@code /topic/**} / {@code /queue/**} SUBSCRIBE 는 건드리지 않는다.
 *
 * @see <a href="file:../../../../../../../../../docs/adr/011-inquiry-domain-with-websocket-stomp.md">ADR-011</a>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    // 클라이언트가 직접 SEND할 수 없는 브로커 경로
    private static final String BROKER_TOPIC_PREFIX = "/topic";
    private static final String BROKER_QUEUE_PREFIX = "/queue";
    // 구독 인가 공통 틀이 붙기 전까지 fail-closed 로 전면 차단하는 chat 엔진 토픽
    private static final String CHAT_TOPIC_PREFIX = "/topic/chat";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        String destination = accessor.getDestination();

        // 브로커 경로로 직접 SEND 하는 것은 차단(broadcast는 서버만 수행)
        if (StompCommand.SEND.equals(command) && isBrokerDestination(destination)) {
            throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        }

        // chat 토픽 SUBSCRIBE 는 구독 인가 공통 틀이 붙기 전까지 fail-closed 로 전면 거부
        if (StompCommand.SUBSCRIBE.equals(command) && isChatTopic(destination)) {
            throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
        }

        return message;
    }

    /**
     * 클라이언트가 직접 SEND할 수 없는 브로커 경로인지 확인한다.
     */
    private boolean isBrokerDestination(String destination) {
        return destination != null
            && (hasDestinationPrefix(destination, BROKER_TOPIC_PREFIX)
            || hasDestinationPrefix(destination, BROKER_QUEUE_PREFIX));
    }

    /**
     * 구독 인가 공통 틀이 아직 없어 fail-closed 로 차단하는 chat 엔진 토픽인지 확인
     */
    private boolean isChatTopic(String destination) {
        return destination != null && hasDestinationPrefix(destination, CHAT_TOPIC_PREFIX);
    }

    private boolean hasDestinationPrefix(String destination, String prefix) {
        return destination.equals(prefix) || destination.startsWith(prefix + "/");
    }
}
