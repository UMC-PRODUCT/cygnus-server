package com.umc.product.community.adapter.in.websocket;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CommunityStompDestinationParser {

    private static final String POSITIVE_ID = "([1-9][0-9]*)";
    private static final Pattern MESSAGE_CREATE = Pattern.compile(
        "^/app/community/threads/" + POSITIVE_ID + "/messages$"
    );
    private static final Pattern MESSAGE_EDIT = Pattern.compile(
        "^/app/community/threads/" + POSITIVE_ID + "/messages/" + POSITIVE_ID + "/edit$"
    );
    private static final Pattern MESSAGE_DELETE = Pattern.compile(
        "^/app/community/threads/" + POSITIVE_ID + "/messages/" + POSITIVE_ID + "/delete$"
    );
    private static final Pattern REACTION_ADD = Pattern.compile(
        "^/app/community/threads/" + POSITIVE_ID + "/messages/" + POSITIVE_ID + "/reactions/add$"
    );
    private static final Pattern REACTION_REMOVE = Pattern.compile(
        "^/app/community/threads/" + POSITIVE_ID + "/messages/" + POSITIVE_ID + "/reactions/remove$"
    );
    private static final Pattern READ_UPDATE = Pattern.compile(
        "^/app/community/threads/" + POSITIVE_ID + "/read$"
    );
    private static final Pattern THREAD_SUBSCRIPTION = Pattern.compile(
        "^/topic/community/threads/" + POSITIVE_ID + "/members/" + POSITIVE_ID + "/events$"
    );
    private static final Pattern PERSONAL_SUBSCRIPTION = Pattern.compile(
        "^/topic/community/members/" + POSITIVE_ID + "/events$"
    );

    private CommunityStompDestinationParser() {
    }

    static Optional<SendDestination> parseSend(String destination) {
        if (destination == null) {
            return Optional.empty();
        }

        Optional<SendDestination> parsed = matchSend(
            MESSAGE_CREATE,
            destination,
            CommunityStompCommandType.MESSAGE_CREATE
        );
        if (parsed.isPresent()) {
            return parsed;
        }
        parsed = matchSend(MESSAGE_EDIT, destination, CommunityStompCommandType.MESSAGE_EDIT);
        if (parsed.isPresent()) {
            return parsed;
        }
        parsed = matchSend(MESSAGE_DELETE, destination, CommunityStompCommandType.MESSAGE_DELETE);
        if (parsed.isPresent()) {
            return parsed;
        }
        parsed = matchSend(REACTION_ADD, destination, CommunityStompCommandType.REACTION_ADD);
        if (parsed.isPresent()) {
            return parsed;
        }
        parsed = matchSend(REACTION_REMOVE, destination, CommunityStompCommandType.REACTION_REMOVE);
        if (parsed.isPresent()) {
            return parsed;
        }
        return matchSend(READ_UPDATE, destination, CommunityStompCommandType.READ_UPDATE);
    }

    static Optional<SubscriptionDestination> parseSubscription(String destination) {
        if (destination == null) {
            return Optional.empty();
        }

        Matcher threadMatcher = THREAD_SUBSCRIPTION.matcher(destination);
        if (threadMatcher.matches()) {
            try {
                return Optional.of(new ThreadSubscription(
                    Long.parseLong(threadMatcher.group(1)),
                    Long.parseLong(threadMatcher.group(2))
                ));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        Matcher personalMatcher = PERSONAL_SUBSCRIPTION.matcher(destination);
        if (personalMatcher.matches()) {
            try {
                return Optional.of(new PersonalSubscription(Long.parseLong(personalMatcher.group(1))));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    static String memberThreadTopic(Long threadId, Long memberId) {
        return "/topic/community/threads/%d/members/%d/events".formatted(threadId, memberId);
    }

    private static Optional<SendDestination> matchSend(
        Pattern pattern,
        String destination,
        CommunityStompCommandType command
    ) {
        Matcher matcher = pattern.matcher(destination);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            Long threadId = Long.parseLong(matcher.group(1));
            Long messageId = matcher.groupCount() == 2 ? Long.parseLong(matcher.group(2)) : null;
            return Optional.of(new SendDestination(threadId, messageId, command));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    record SendDestination(
        Long threadId,
        Long messageId,
        CommunityStompCommandType command
    ) {
    }

    sealed interface SubscriptionDestination permits ThreadSubscription, PersonalSubscription {
    }

    record ThreadSubscription(Long threadId, Long memberId) implements SubscriptionDestination {
    }

    record PersonalSubscription(Long memberId) implements SubscriptionDestination {
    }
}
