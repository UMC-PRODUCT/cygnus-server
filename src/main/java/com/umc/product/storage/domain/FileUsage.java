package com.umc.product.storage.domain;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_usage")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(name = "file_id", nullable = false, updatable = false)
    private String fileId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private FileUsage(Long ownerId, String fileId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("파일 usage owner ID는 필수입니다.");
        }
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("파일 usage file ID는 필수입니다.");
        }
        this.ownerId = ownerId;
        this.fileId = fileId;
    }

    public static FileUsage of(Long ownerId, String fileId) {
        return new FileUsage(ownerId, fileId);
    }
}
