package com.umc.product.global.logging;

import java.util.Iterator;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

/**
 * 로그 이벤트를 정제한 뒤 하위 어펜더들로 전달하는 위임 어펜더다.
 *
 * <p>마스킹을 어펜더 계층에 두는 이유는 두 가지다.
 *
 * <ol>
 *     <li><b>레벨·필터 판정 이후에 실행된다.</b> 이전 구현({@code TurboFilter})은 레벨 검사보다 먼저
 *     실행되어, 출력될 리 없는 로그까지 정제하다 {@code StackOverflowError}로 애플리케이션을
 *     중단시켰다. 어펜더는 logback이 레벨과 필터 체인을 모두 통과시킨 이벤트만 받는다.</li>
 *     <li><b>모든 어펜더를 덮는다.</b> 인코더 계층 마스킹(logstash {@code MaskingJsonGeneratorDecorator},
 *     커스텀 {@code MessageConverter})은 JSON 또는 레이아웃 기반 어펜더에만 적용되어 OTLP 경로가
 *     비어버린다.</li>
 * </ol>
 *
 * <p>사용 예:
 * <pre>{@code
 * <appender name="SANITIZED" class="com.umc.product.global.logging.SanitizingAppender">
 *     <appender-ref ref="CONSOLE_JSON"/>
 *     <appender-ref ref="OTEL"/>
 * </appender>
 * }</pre>
 */
public class SanitizingAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
    implements AppenderAttachable<ILoggingEvent> {

    private final AppenderAttachableImpl<ILoggingEvent> delegates = new AppenderAttachableImpl<>();

    @Override
    protected void append(ILoggingEvent event) {
        delegates.appendLoopOnAppenders(SanitizedLoggingEvent.wrap(event));
    }

    @Override
    public void start() {
        if (isStarted()) {
            return;
        }
        if (!delegates.iteratorForAppenders().hasNext()) {
            // 하위 어펜더가 없으면 로그가 조용히 사라진다. 배선 실수를 시작 시점에 드러낸다.
            addError("SanitizingAppender [" + getName() + "] 에 하위 어펜더가 없어 로그가 유실됩니다.");
            return;
        }
        super.start();
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        super.stop();
        delegates.detachAndStopAllAppenders();
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        addInfo("SanitizingAppender [" + getName() + "] 에 [" + newAppender.getName() + "] 연결");
        delegates.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return delegates.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return delegates.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return delegates.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        delegates.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return delegates.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return delegates.detachAppender(name);
    }
}
