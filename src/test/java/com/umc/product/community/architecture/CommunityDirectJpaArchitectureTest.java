package com.umc.product.community.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.umc.product.common.BaseEntity;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Scrap;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@DisplayName("Community 직접 JPA 아키텍처")
class CommunityDirectJpaArchitectureTest {

    private static final Path COMMUNITY_SOURCE_ROOT = Path.of(
        "src/main/java/com/umc/product/community"
    );
    private static final Path COMMUNITY_QUERYDSL_ROOT = Path.of(
        "build/generated/querydsl/com/umc/product/community"
    );
    private static final String LEGACY_ENTITY_PACKAGE =
        "com.umc.product.community.adapter.out.persistence.entity";
    private static final List<DirectEntity> DIRECT_ENTITIES = List.of(
        new DirectEntity(Post.class, "post"),
        new DirectEntity(Comment.class, "comment"),
        new DirectEntity(Scrap.class, "scrap")
    );
    private static final List<String> LEGACY_SOURCE_SIMPLE_NAMES = List.of(
        "PostJpaEntity",
        "CommentJpaEntity",
        "ScrapJpaEntity",
        "PostLikeJpaEntity",
        "PostLikeRepository"
    );

    @Test
    @DisplayName("Post Comment Scrap은 직접 JPA 엔티티와 domain QueryDSL 타입을 갖는다")
    void post_comment_scrap은_직접_JPA_엔티티와_domain_querydsl_타입을_갖는다() {
        // given / when / then
        DIRECT_ENTITIES.forEach(this::assertDirectJpaEntity);
    }

    @Test
    @DisplayName("Post Comment Scrap의 legacy JpaEntity와 QueryDSL 산출물은 남아있지 않다")
    void post_comment_scrap의_legacy_jpa_entity와_querydsl_산출물은_남아있지_않다() throws IOException {
        // given
        List<String> legacySimpleNames = DIRECT_ENTITIES.stream()
            .map(entity -> entity.domainType().getSimpleName() + "JpaEntity")
            .toList();
        List<String> legacyQuerydslSimpleNames = DIRECT_ENTITIES.stream()
            .map(entity -> "Q" + entity.domainType().getSimpleName() + "JpaEntity")
            .toList();
        List<String> legacyReferenceNames = Stream.concat(
            legacySimpleNames.stream(),
            Stream.of("PostLikeJpaEntity", "PostLikeRepository")
        ).toList();

        // when
        List<Path> legacySources = findFiles(COMMUNITY_SOURCE_ROOT)
            .filter(path -> LEGACY_SOURCE_SIMPLE_NAMES.contains(
                path.getFileName().toString().replace(".java", "")
            ))
            .toList();
        List<Path> legacyQuerydslSources = findFiles(COMMUNITY_QUERYDSL_ROOT)
            .filter(path -> legacyQuerydslSimpleNames.contains(path.getFileName().toString().replace(".java", "")))
            .toList();
        List<String> legacyImports = findProductionJavaFiles()
            .flatMap(path -> legacyTypeReferences(path, legacyReferenceNames, legacyQuerydslSimpleNames))
            .toList();

        // then
        assertThat(legacySources).isEmpty();
        assertThat(legacyQuerydslSources).isEmpty();
        assertThat(legacyImports).isEmpty();
        DIRECT_ENTITIES.forEach(entity -> assertNoLegacyClassResource(entity.domainType()));
        assertNoLegacyClassResource("PostLikeJpaEntity");
    }

    private void assertDirectJpaEntity(DirectEntity entity) {
        Class<?> domainType = entity.domainType();

        assertThat(domainType.isAnnotationPresent(Entity.class))
            .as("%s에 @Entity가 있어야 한다", domainType.getSimpleName())
            .isTrue();
        assertThat(domainType.getSuperclass())
            .as("%s는 BaseEntity를 직접 상속해야 한다", domainType.getSimpleName())
            .isEqualTo(BaseEntity.class);
        assertThat(domainType.getAnnotation(Table.class).name())
            .as("%s의 테이블 이름", domainType.getSimpleName())
            .isEqualTo(entity.tableName());

        Field idField = Arrays.stream(domainType.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(Id.class))
            .findFirst()
            .orElseThrow(() -> new AssertionError(domainType.getSimpleName() + "에 @Id 필드가 없습니다."));
        assertThat(idField.getType()).isEqualTo(Long.class);

        assertThat(Arrays.stream(domainType.getDeclaredFields())
            .filter(this::hasAggregateRelation)
            .toList())
            .as("%s는 다른 aggregate를 JPA relation으로 직접 보유하면 안 된다", domainType.getSimpleName())
            .isEmpty();

        assertThat(Arrays.stream(domainType.getMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> method.getParameterCount() == 0)
            .filter(method -> Collection.class.isAssignableFrom(method.getReturnType()))
            .toList())
            .as("%s는 영속 컬렉션을 public getter로 노출하면 안 된다", domainType.getSimpleName())
            .isEmpty();

        String queryTypeName = domainType.getPackageName() + ".Q" + domainType.getSimpleName();
        assertThatCode(() -> {
            Class<?> queryType = Class.forName(queryTypeName, true, domainType.getClassLoader());
            assertThat(EntityPathBase.class.isAssignableFrom(queryType))
                .as("%s는 QueryDSL EntityPathBase여야 한다", queryTypeName)
                .isTrue();
        }).doesNotThrowAnyException();
    }

    private boolean hasAggregateRelation(Field field) {
        return field.isAnnotationPresent(ManyToMany.class)
            || field.isAnnotationPresent(ManyToOne.class)
            || field.isAnnotationPresent(OneToMany.class)
            || field.isAnnotationPresent(OneToOne.class);
    }

    private void assertNoLegacyClassResource(Class<?> domainType) {
        String legacySimpleName = domainType.getSimpleName() + "JpaEntity";
        assertNoLegacyClassResource(legacySimpleName, "Q" + legacySimpleName);
    }

    private void assertNoLegacyClassResource(String... legacyClassNames) {
        ClassLoader classLoader = getClass().getClassLoader();
        for (String legacyClassName : legacyClassNames) {
            String resourceName = LEGACY_ENTITY_PACKAGE.replace('.', '/')
                + "/" + legacyClassName + ".class";
            try {
                List<URL> resources = Collections.list(
                    classLoader.getResources(resourceName)
                );
                assertThat(resources)
                    .as("legacy class resource %s", resourceName)
                    .isEmpty();
            } catch (IOException e) {
                throw new IllegalStateException("legacy class resource를 읽지 못했습니다: " + resourceName, e);
            }
        }
    }

    private Stream<Path> findProductionJavaFiles() throws IOException {
        return findFiles(COMMUNITY_SOURCE_ROOT);
    }

    private Stream<Path> findFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return Stream.empty();
        }
        return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
    }

    private Stream<String> legacyTypeReferences(
        Path path,
        List<String> legacySimpleNames,
        List<String> legacyQuerydslSimpleNames
    ) {
        try {
            return Files.readAllLines(path).stream()
                .filter(line -> legacySimpleNames.stream().anyMatch(line::contains)
                    || legacyQuerydslSimpleNames.stream().anyMatch(line::contains))
                .map(line -> path + ":" + line.trim());
        } catch (IOException e) {
            throw new IllegalStateException("community production source를 읽지 못했습니다: " + path, e);
        }
    }

    private record DirectEntity(Class<?> domainType, String tableName) {
    }
}
