package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.BaseEntity;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@DisplayName("Community thread 직접 JPA 매핑")
class CommunityThreadJpaMappingTest {

    @Test
    @DisplayName("Thread와 Member는 BaseEntity를 상속한 직접 JPA 엔티티다")
    void directJpaEntities_별도_영속_엔티티가_없다() {
        assertDirectEntity(CommunityThread.class, "community_thread");
        assertDirectEntity(CommunityThreadMember.class, "community_thread_member");
    }

    @Test
    @DisplayName("Thread와 Member에는 OneToMany와 cross-domain 객체 참조가 없다")
    void scalarBoundaries_oneToMany와_cross_domain_참조가_없다() {
        assertThat(Arrays.stream(CommunityThread.class.getDeclaredFields()))
            .noneMatch(field -> field.isAnnotationPresent(OneToMany.class));
        assertThat(Arrays.stream(CommunityThreadMember.class.getDeclaredFields()))
            .noneMatch(field -> field.isAnnotationPresent(OneToMany.class));
        assertThat(Arrays.stream(CommunityThread.class.getDeclaredFields()).map(Field::getType))
            .allMatch(this::isScalarOrCommunityEnum);
        assertThat(Arrays.stream(CommunityThreadMember.class.getDeclaredFields()).map(Field::getType))
            .allMatch(this::isScalarOrCommunityEnum);
    }

    private void assertDirectEntity(Class<?> entityType, String tableName) {
        assertThat(entityType.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(entityType.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(entityType.getAnnotation(Table.class).name()).isEqualTo(tableName);
        assertThat(Arrays.stream(entityType.getDeclaredFields()))
            .anyMatch(field -> field.isAnnotationPresent(Id.class) && field.getType().equals(Long.class));
    }

    private boolean isScalarOrCommunityEnum(Class<?> type) {
        return type.isPrimitive()
            || type.getPackageName().startsWith("java.")
            || type.getPackageName().equals("com.umc.product.community.domain.enums");
    }
}
