package com.umc.product.project.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectCommand;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.SaveProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileUsagePort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.support.IntegrationTestSupport;

@Import(ProjectFileUsageRollbackIntegrationTest.FailingUsageSaveConfig.class)
@DisplayName("프로젝트 파일 usage 저장 실패 rollback 통합")
class ProjectFileUsageRollbackIntegrationTest extends IntegrationTestSupport {

    private static final Long REQUESTER_ID = 7L;

    @Autowired
    private UpdateProjectUseCase updateProjectUseCase;

    @Autowired
    private SaveProjectPort saveProjectPort;

    @Autowired
    private LoadProjectPort loadProjectPort;

    @Autowired
    private SaveFileMetadataPort saveFileMetadataPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void usage_row_저장이_실패하면_project_mutation과_owner_anchor도_함께_rollback된다() {
        // given
        Project project = saveProjectPort.save(
            Project.createDraft(1L, 2L, REQUESTER_ID, 3L, REQUESTER_ID));
        saveConfirmedFile("project-logo");

        // when & then
        assertThatThrownBy(() -> updateProjectUseCase.update(UpdateProjectCommand.builder()
            .projectId(project.getId())
            .requesterMemberId(REQUESTER_ID)
            .name("변경 이름")
            .logoFileId("project-logo")
            .build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("usage save failed");
        Project rolledBack = loadProjectPort.findById(project.getId()).orElseThrow();
        assertThat(rolledBack.getName()).isNull();
        assertThat(rolledBack.getLogoFileId()).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_usage", Long.class))
            .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_usage_owner", Long.class))
            .isZero();
    }

    private void saveConfirmedFile(String fileId) {
        FileMetadata metadata = FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileId + ".png")
            .category(FileCategory.PROJECT_LOGO)
            .contentType("image/png")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("project-rollback/" + fileId + ".png")
            .uploadedMemberId(REQUESTER_ID)
            .build();
        metadata.markAsUploaded(Instant.parse("2026-07-17T00:00:00Z"));
        saveFileMetadataPort.save(metadata);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingUsageSaveConfig {

        @Bean
        @Primary
        SaveFileUsagePort failingSaveFileUsagePort() {
            return new SaveFileUsagePort() {
                @Override
                public void addUsages(Long ownerId, Set<String> fileIds) {
                    throw new IllegalStateException("usage save failed");
                }

                @Override
                public void removeUsages(Long ownerId, Set<String> fileIds) {
                    throw new UnsupportedOperationException("unexpected usage removal");
                }
            };
        }
    }
}
