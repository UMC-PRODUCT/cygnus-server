package com.umc.product.storage.domain;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_usage_owner")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileUsageOwner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usage_namespace", nullable = false, updatable = false, length = 100)
    private String usageNamespace;

    @Column(name = "resource_key", nullable = false, updatable = false, length = 128)
    private String resourceKey;

    @Column(nullable = false, updatable = false, length = 50)
    private String slot;

    private FileUsageOwner(FileUsageCoordinate coordinate) {
        this.usageNamespace = coordinate.usageNamespace();
        this.resourceKey = coordinate.resourceKey();
        this.slot = coordinate.slot();
    }

    public static FileUsageOwner from(FileUsageCoordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("파일 usage owner 좌표는 필수입니다.");
        }
        return new FileUsageOwner(coordinate);
    }

    public FileUsageCoordinate coordinate() {
        return FileUsageCoordinate.of(usageNamespace, resourceKey, slot);
    }
}
