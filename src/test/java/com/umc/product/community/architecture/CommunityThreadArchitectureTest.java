package com.umc.product.community.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Community Thread 경계와 transport 계약 아키텍처")
class CommunityThreadArchitectureTest {

    private static final Path PRODUCTION_SOURCE_ROOT = Path.of("src/main/java/com/umc/product");
    private static final Path COMMUNITY_SOURCE_ROOT = PRODUCTION_SOURCE_ROOT.resolve("community");
    private static final Path COMMUNITY_ADAPTER_IN_ROOT = COMMUNITY_SOURCE_ROOT.resolve("adapter/in");
    private static final Path CHAT_ADAPTER_IN_ROOT = PRODUCTION_SOURCE_ROOT.resolve("chat/adapter/in");
    private static final Path GLOBAL_SOURCE_ROOT = PRODUCTION_SOURCE_ROOT.resolve("global");
    private static final Set<Path> ALLOWED_CHAT_EVENT_IMPORT_SOURCES = Set.of(
        COMMUNITY_SOURCE_ROOT.resolve("adapter/in/event/CommunityThreadRealtimeEventListener.java"),
        COMMUNITY_SOURCE_ROOT.resolve("application/port/in/realtime/RelayCommunityThreadRealtimeEventUseCase.java"),
        COMMUNITY_SOURCE_ROOT.resolve("application/service/realtime/CommunityThreadRealtimeFanOutService.java"),
        COMMUNITY_SOURCE_ROOT.resolve("application/service/realtime/CommunityThreadChatRealtimeRelay.java")
    );
    private static final Set<String> ALLOWED_CHAT_EVENT_TYPES = Set.of(
        "com.umc.product.chat.domain.event.ChatMessageCreatedEvent",
        "com.umc.product.chat.domain.event.ChatMessageUpdatedEvent",
        "com.umc.product.chat.domain.event.ChatMessageDeletedEvent",
        "com.umc.product.chat.domain.event.ChatMessageReactionChangedEvent",
        "com.umc.product.chat.domain.event.ChatReadUpdatedEvent"
    );

    /**
     * Community가 Chat command/query 계약을 채우기 위해 값으로만 쓰는 Chat domain enum. entity/repository 참조가 아니므로
     * 경계 위반이 아니며, 소문자 import 문 전체를 정확히 일치시킨다.
     */
    private static final Set<String> ALLOWED_CHAT_DOMAIN_VALUE_IMPORTS = Set.of(
        "import com.umc.product.chat.domain.messagecontenttype;",
        "import com.umc.product.chat.domain.chatroomreadscope;"
    );

    private static final Set<String> ALLOWED_DOMAIN_ROOTS = Set.of(
        "community",
        "common",
        "authorization",
        "audit"
    );
    private static final Pattern DOMAIN_IMPORT = Pattern.compile(
        "com\\.umc\\.product\\.([a-z0-9_]+)\\.domain\\.",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STOMP_SHARED_TOPIC = Pattern.compile(
        "topic/community/threads/[^\"'\\r\\n]*events",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TYPING_OR_READERS = Pattern.compile(
        "\\btyping(?:indicator)?\\b|\\breaders?\\b",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STOMP_MAPPING = Pattern.compile(
        "@(Post|Patch|Delete)Mapping\\s*\\([^)]*\\)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Test
    @DisplayName(
        "Community는 승인된 realtime Chat event 외의 foreign persistence/adapter를 직접 참조하지 않는다"
    )
    void community_does_not_cross_persistence_or_chat_adapter_boundary() throws IOException {
        List<String> foreignPersistenceImports = sourceFiles(COMMUNITY_SOURCE_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> isForeignPersistenceImport(line.path(), line.content()))
            .map(SourceLine::describe)
            .toList();

        List<String> controllerChatImports = sourceFiles(COMMUNITY_ADAPTER_IN_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> line.content().contains("com.umc.product.chat."))
            .filter(line -> !isAllowedChatEventImport(line.path(), line.content()))
            .map(SourceLine::describe)
            .toList();

        assertThat(foreignPersistenceImports)
            .as(
                "Community production source의 foreign repository/entity import "
                    + "(realtime Chat event는 승인된 4개 source와 5개 FQCN만 허용)"
            )
            .isEmpty();
        assertThat(controllerChatImports)
            .as(
                "Community inbound adapter의 Chat 직접 참조 "
                    + "(realtime listener의 승인된 5개 event import만 허용)"
            )
            .isEmpty();
        assertThat(javaSources(CHAT_ADAPTER_IN_ROOT))
            .as("Chat engine에는 public REST/STOMP adapter/in이 없어야 한다")
            .isEmpty();
    }

    @Test
    @DisplayName("Community aggregate와 thread persistence에는 OneToMany가 없다")
    void community_does_not_introduce_one_to_many() throws IOException {
        List<String> violations = sourceFiles(COMMUNITY_SOURCE_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> line.content().contains("@OneToMany")
                || line.content().contains("jakarta.persistence.OneToMany"))
            .map(SourceLine::describe)
            .toList();

        assertThat(violations)
            .as("Community domain/adapter의 @OneToMany")
            .isEmpty();
    }

    @Test
    @DisplayName("raw chatRoomId와 shared thread topic은 외부 Community 계약에 노출되지 않는다")
    void raw_room_id_and_shared_topic_are_forbidden_at_external_boundary() throws IOException {
        List<String> rawRoomId = sourceFiles(COMMUNITY_ADAPTER_IN_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> line.content().toLowerCase(Locale.ROOT).matches(".*chat[_]?room[_]?id.*")
                || line.content().matches(".*\\broomId\\b.*"))
            .map(SourceLine::describe)
            .toList();

        List<String> sharedTopic = sourceFiles(COMMUNITY_SOURCE_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> STOMP_SHARED_TOPIC.matcher(line.content()).find())
            .filter(line -> !line.content().contains("/members/"))
            .map(SourceLine::describe)
            .toList();

        assertThat(rawRoomId)
            .as("Community inbound adapter의 raw Chat room identifier")
            .isEmpty();
        assertThat(sharedTopic)
            .as("kick 이후 stale subscription leak을 허용하는 shared thread topic")
            .isEmpty();
    }

    @Test
    @DisplayName("Community에는 notification delivery, Chat hard delete, typing/readers 기능이 없다")
    void community_does_not_add_notification_or_chat_lifecycle_side_effects() throws IOException {
        List<String> notificationDelivery = sourceFiles(COMMUNITY_SOURCE_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> containsNotificationDeliveryToken(line.content()))
            .map(SourceLine::describe)
            .toList();
        List<String> hardDelete = sourceFiles(COMMUNITY_SOURCE_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> line.content().contains("DeleteChatRoomUseCase")
                || line.content().contains("deleteChatRoom(")
                || line.content().contains("hardDeleteChatRoom"))
            .map(SourceLine::describe)
            .toList();
        List<String> typingOrReaders = sourceFiles(COMMUNITY_SOURCE_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> TYPING_OR_READERS.matcher(line.content()).find())
            .map(SourceLine::describe)
            .toList();

        assertThat(notificationDelivery)
            .as("Community의 notification delivery 변경")
            .isEmpty();
        assertThat(hardDelete)
            .as("Community thread soft delete에서 Chat hard delete 호출")
            .isEmpty();
        assertThat(typingOrReaders)
            .as("이번 범위에 포함하지 않는 typing/readers surface")
            .isEmpty();
    }

    @Test
    @DisplayName("global ObjectMapper에는 Community 전용 serializer가 추가되지 않는다")
    void global_object_mapper_has_no_community_specific_serializer() throws IOException {
        List<String> violations = sourceFiles(GLOBAL_SOURCE_ROOT).stream()
            .filter(source -> source.content().contains("ObjectMapper")
                && source.content().toLowerCase(Locale.ROOT).contains("community"))
            .map(source -> source.path() + " contains ObjectMapper and Community")
            .toList();

        assertThat(violations)
            .as("global ObjectMapper에 Community 전용 serializer/deserializer를 넣은 파일")
            .isEmpty();
    }

    @Test
    @DisplayName("Community admin path와 REST 금지 mutation이 transport 경계를 지킨다")
    void admin_namespace_and_rest_mutation_boundary_are_enforced() throws IOException {
        List<String> topLevelAdminPaths = sourceFiles(COMMUNITY_ADAPTER_IN_ROOT).stream()
            .flatMap(source -> source.lines().map(line -> new SourceLine(source.path(), line)))
            .filter(line -> line.content().contains("/api/v1/admin"))
            .map(SourceLine::describe)
            .toList();
        List<String> forbiddenMutations = sourceFiles(COMMUNITY_ADAPTER_IN_ROOT).stream()
            .flatMap(source -> mappingDeclarations(source).stream()
                .map(mapping -> new SourceLine(source.path(), mapping)))
            .filter(line -> isForbiddenRestMutation(line.content()))
            .map(SourceLine::describe)
            .toList();

        assertThat(topLevelAdminPaths)
            .as("top-level /api/v1/admin Community path")
            .isEmpty();
        assertThat(forbiddenMutations)
            .as("REST message/reaction/read mutation mapping")
            .isEmpty();
    }

    private static boolean isForeignPersistenceImport(Path sourcePath, String line) {
        if (!line.trim().startsWith("import ")) {
            return false;
        }
        String importLine = line.trim().toLowerCase(Locale.ROOT);
        if (importLine.startsWith("import com.umc.product.chat.domain.event.")) {
            return !isAllowedChatEventImport(sourcePath, line);
        }
        if (importLine.contains("com.umc.product.chat.adapter.")) {
            return true;
        }
        if (importLine.contains(".adapter.out.persistence.")) {
            return true;
        }
        if (ALLOWED_CHAT_DOMAIN_VALUE_IMPORTS.contains(importLine)) {
            return false;
        }
        if (importLine.contains(".chat.domain.")) {
            return true;
        }
        Matcher domainImport = DOMAIN_IMPORT.matcher(importLine);
        return domainImport.find()
            && !ALLOWED_DOMAIN_ROOTS.contains(domainImport.group(1))
            && !importLine.contains(".domain.event.");
    }

    private static boolean isAllowedChatEventImport(Path sourcePath, String line) {
        String importLine = line.trim();
        if (!importLine.startsWith("import ") || !importLine.endsWith(";")) {
            return false;
        }
        String importedType = importLine.substring("import ".length(), importLine.length() - 1).trim();
        return ALLOWED_CHAT_EVENT_IMPORT_SOURCES.contains(sourcePath.normalize())
            && ALLOWED_CHAT_EVENT_TYPES.contains(importedType);
    }

    private static boolean containsNotificationDeliveryToken(String line) {
        String code = line.replaceAll("//.*", "");
        return code.contains("com.umc.product.notification.")
            || code.matches(".*\\b(?:send|deliver|publish)(?:Notification|Alarm)\\b.*")
            || code.matches(".*\\bNotification(?:Service|Publisher|Sender)\\b.*");
    }

    private static boolean isForbiddenRestMutation(String mapping) {
        String normalized = mapping.toLowerCase(Locale.ROOT);
        boolean messageCreate = normalized.contains("@postmapping")
            && normalized.contains("/threads/{threadid}/messages")
            && !normalized.contains("/report");
        boolean messageEditOrDelete = (normalized.contains("@patchmapping")
            || normalized.contains("@deletemapping"))
            && normalized.contains("/messages/")
            && !normalized.contains("/report");
        boolean reactionMutation = (normalized.contains("@postmapping")
            || normalized.contains("@deletemapping"))
            && normalized.contains("/reactions/");
        boolean readMutation = (normalized.contains("@postmapping")
            || normalized.contains("@patchmapping")
            || normalized.contains("@deletemapping"))
            && normalized.contains("/read");
        return messageCreate || messageEditOrDelete || reactionMutation || readMutation;
    }

    private static List<String> mappingDeclarations(SourceFile source) {
        return STOMP_MAPPING.matcher(source.content()).results()
            .map(match -> match.group())
            .toList();
    }

    private static List<SourceFile> sourceFiles(Path root) throws IOException {
        return javaSources(root).stream()
            .map(path -> {
                try {
                    return new SourceFile(path, Files.readString(path));
                } catch (IOException exception) {
                    throw new IllegalStateException("production source를 읽지 못했습니다: " + path, exception);
                }
            })
            .toList();
    }

    private static List<Path> javaSources(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var paths = Files.walk(root)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }
    }

    private record SourceFile(Path path, String content) {
        private java.util.stream.Stream<String> lines() {
            return content.lines();
        }
    }

    private record SourceLine(Path path, String content) {
        private String describe() {
            return path + ":" + content.trim();
        }
    }
}
