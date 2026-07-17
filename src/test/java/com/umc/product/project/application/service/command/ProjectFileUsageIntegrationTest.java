package com.umc.product.project.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.project.application.port.in.command.DeleteProjectUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.application.port.in.command.dto.DeleteProjectCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectCommand;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.SaveProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("프로젝트 logo/thumbnail FileUsageRegistry 통합")
class ProjectFileUsageIntegrationTest extends IntegrationTestSupport {

    private static final Long REQUESTER_ID = 7L;
    private static final Instant CONFIRMED_AT = Instant.parse("2026-07-17T00:00:00Z");

    @Autowired
    private UpdateProjectUseCase updateProjectUseCase;

    @Autowired
    private DeleteProjectUseCase deleteProjectUseCase;

    @Autowired
    private SaveProjectPort saveProjectPort;

    @Autowired
    private LoadProjectPort loadProjectPort;

    @Autowired
    private SaveFileMetadataPort saveFileMetadataPort;

    @Autowired
    private LoadFileMetadataPort loadFileMetadataPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void thumbnail만_교체해도_logo_snapshot은_독립적으로_유지되고_null은_keep한다() {
        // given
        Project project = saveProject();
        saveConfirmedFile("logo-file", REQUESTER_ID, FileCategory.PROJECT_LOGO);
        saveConfirmedFile("thumbnail-old", REQUESTER_ID, FileCategory.PROJECT_THUMBNAIL);
        saveConfirmedFile("thumbnail-new", REQUESTER_ID, FileCategory.PROJECT_THUMBNAIL);
        updateProjectUseCase.update(updateCommand(
            project.getId(), "logo-file", "thumbnail-old"));

        // when
        updateProjectUseCase.update(updateCommand(
            project.getId(), null, "thumbnail-new"));

        // then
        assertThat(usageSnapshot(projectFile(project.getId(), "logo")))
            .containsExactly("logo-file");
        assertThat(usageSnapshot(projectFile(project.getId(), "thumbnail")))
            .containsExactly("thumbnail-new");
        Project updated = loadProjectPort.findById(project.getId()).orElseThrow();
        assertThat(updated.getLogoFileId()).isEqualTo("logo-file");
        assertThat(updated.getThumbnailFileId()).isEqualTo("thumbnail-new");
        assertThat(loadFileMetadataPort.findByFileId("thumbnail-old").orElseThrow().getUnreferencedAt())
            .isNotNull();
    }

    @Test
    void hard_delete는_project_삭제_전에_logo와_thumbnail_usage를_모두_detach한다() {
        // given
        Project project = saveProject();
        saveConfirmedFile("logo-file", REQUESTER_ID, FileCategory.PROJECT_LOGO);
        saveConfirmedFile("thumbnail-file", REQUESTER_ID, FileCategory.PROJECT_THUMBNAIL);
        updateProjectUseCase.update(updateCommand(
            project.getId(), "logo-file", "thumbnail-file"));

        // when
        deleteProjectUseCase.delete(DeleteProjectCommand.builder()
            .projectId(project.getId())
            .requesterMemberId(REQUESTER_ID)
            .build());

        // then
        assertThat(loadProjectPort.findById(project.getId())).isEmpty();
        assertThat(usageSnapshot(projectFile(project.getId(), "logo"))).isEmpty();
        assertThat(usageSnapshot(projectFile(project.getId(), "thumbnail"))).isEmpty();
        assertThat(loadFileMetadataPort.findByFileId("logo-file").orElseThrow().getUnreferencedAt())
            .isNotNull();
        assertThat(loadFileMetadataPort.findByFileId("thumbnail-file").orElseThrow().getUnreferencedAt())
            .isNotNull();
    }

    @Test
    void 다른_uploader의_logo_attach가_실패하면_project_mutation도_rollback된다() {
        // given
        Project project = saveProject();
        saveConfirmedFile("old-logo", REQUESTER_ID, FileCategory.PROJECT_LOGO);
        saveConfirmedFile("other-logo", 99L, FileCategory.PROJECT_LOGO);
        updateProjectUseCase.update(updateCommand(project.getId(), "old-logo", null));

        // when & then
        assertThatThrownBy(() -> updateProjectUseCase.update(
            UpdateProjectCommand.builder()
                .projectId(project.getId())
                .requesterMemberId(REQUESTER_ID)
                .name("변경 이름")
                .logoFileId("other-logo")
                .build()))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_USE_FORBIDDEN);
        Project rolledBack = loadProjectPort.findById(project.getId()).orElseThrow();
        assertThat(rolledBack.getName()).isNull();
        assertThat(rolledBack.getLogoFileId()).isEqualTo("old-logo");
        assertThat(usageSnapshot(projectFile(project.getId(), "logo")))
            .containsExactly("old-logo");
    }

    private Project saveProject() {
        return saveProjectPort.save(Project.createDraft(1L, 2L, REQUESTER_ID, 3L, REQUESTER_ID));
    }

    private UpdateProjectCommand updateCommand(
        Long projectId,
        String logoFileId,
        String thumbnailFileId
    ) {
        return UpdateProjectCommand.builder()
            .projectId(projectId)
            .requesterMemberId(REQUESTER_ID)
            .logoFileId(logoFileId)
            .thumbnailFileId(thumbnailFileId)
            .build();
    }

    private FileUsageCoordinate projectFile(Long projectId, String slot) {
        return FileUsageCoordinate.of("project", projectId.toString(), slot);
    }

    private Set<String> usageSnapshot(FileUsageCoordinate coordinate) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList("""
            SELECT fu.file_id
            FROM file_usage fu
            JOIN file_usage_owner fuo ON fuo.id = fu.owner_id
            WHERE fuo.usage_namespace = ?
              AND fuo.resource_key = ?
              AND fuo.slot = ?
            ORDER BY fu.file_id
            """, String.class, coordinate.usageNamespace(), coordinate.resourceKey(), coordinate.slot()));
    }

    private void saveConfirmedFile(String fileId, Long uploaderId, FileCategory category) {
        FileMetadata metadata = FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileId + ".png")
            .category(category)
            .contentType("image/png")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("project-test/" + fileId + ".png")
            .uploadedMemberId(uploaderId)
            .build();
        metadata.markAsUploaded(CONFIRMED_AT);
        saveFileMetadataPort.save(metadata);
    }
}
