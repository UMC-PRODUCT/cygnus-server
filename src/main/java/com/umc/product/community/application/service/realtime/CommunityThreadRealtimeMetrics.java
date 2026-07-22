package com.umc.product.community.application.service.realtime;

import java.util.Objects;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class CommunityThreadRealtimeMetrics {

    private static final String SEND_METRIC = "community.thread.realtime.send.commands";
    private static final String REJECT_METRIC = "community.thread.realtime.reject.commands";
    private static final String RATE_LIMIT_METRIC = "community.thread.realtime.rate.limit.rejections";
    private static final String FAN_OUT_METRIC = "community.thread.realtime.fanout.events";
    private static final String FAN_OUT_RECIPIENT_METRIC =
        "community.thread.realtime.fanout.recipients";
    private static final String BROADCAST_FAILURE_METRIC =
        "community.thread.realtime.broadcast.failures";
    private static final String BACKFILL_METRIC = "community.thread.realtime.backfill.requests";

    private final MeterRegistry meterRegistry;

    public CommunityThreadRealtimeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSend(Operation operation, Outcome outcome) {
        counter(SEND_METRIC, operation, "outcome", outcome.value()).increment();
    }

    public void recordReject(Operation operation, Reason reason) {
        counter(REJECT_METRIC, operation, "reason", reason.value()).increment();
    }

    public void recordRateLimit(Operation operation) {
        counter(RATE_LIMIT_METRIC, operation, "outcome", Outcome.REJECTED.value()).increment();
    }

    public void recordFanOut(Operation operation, Outcome outcome, int recipientCount) {
        if (recipientCount < 0) {
            throw new IllegalArgumentException("recipientCount must not be negative");
        }
        counter(FAN_OUT_METRIC, operation, "outcome", outcome.value()).increment();
        DistributionSummary.builder(FAN_OUT_RECIPIENT_METRIC)
            .tag("operation", operation.value())
            .tag("outcome", outcome.value())
            .register(meterRegistry)
            .record(recipientCount);
    }

    public void recordBroadcastFailure(Operation operation, Reason reason) {
        counter(BROADCAST_FAILURE_METRIC, operation, "reason", reason.value()).increment();
    }

    public void recordBackfill(Operation operation, Outcome outcome) {
        counter(BACKFILL_METRIC, operation, "outcome", outcome.value()).increment();
    }

    private Counter counter(String name, Operation operation, String bucketName, String bucketValue) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(bucketValue, bucketName + " must not be null");
        return Counter.builder(name)
            .tag("operation", operation.value())
            .tag(bucketName, bucketValue)
            .register(meterRegistry);
    }

    public enum Operation {
        UNKNOWN("unknown"),
        MESSAGE_CREATE("message.create"),
        MESSAGE_EDIT("message.edit"),
        MESSAGE_DELETE("message.delete"),
        REACTION_ADD("reaction.add"),
        REACTION_REMOVE("reaction.remove"),
        READ_UPDATE("read.update"),
        MESSAGE_CREATED("message.created"),
        MESSAGE_UPDATED("message.updated"),
        MESSAGE_DELETED("message.deleted"),
        REACTION_CHANGED("reaction.changed"),
        READ_UPDATED("read.updated"),
        THREAD_INVITED("thread.invited"),
        THREAD_UPDATED("thread.updated"),
        THREAD_DELETED("thread.deleted"),
        MEMBER_KICKED("member.kicked"),
        MEMBER_LEFT("member.left"),
        MESSAGE_HISTORY("message.history"),
        THREAD_DETAIL("thread.detail"),
        THREAD_LIST("thread.list"),
        THREAD_MEMBERS("thread.members");

        private final String value;

        Operation(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Outcome {
        SUCCESS("success"),
        FAILURE("failure"),
        REJECTED("rejected"),
        SKIPPED("skipped");

        private final String value;

        Outcome(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Reason {
        VALIDATION("validation"),
        AUTHENTICATION("authentication"),
        AUTHORIZATION("authorization"),
        NOT_FOUND("not_found"),
        CONFLICT("conflict"),
        RATE_LIMIT("rate_limit"),
        APPLICATION("application"),
        BROKER_UNAVAILABLE("broker_unavailable"),
        UNKNOWN("unknown");

        private final String value;

        Reason(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
