package com.umc.product.notice.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.notice.application.port.in.command.ManageNoticeContentUseCase;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeImagesCommand;
import com.umc.product.notice.application.port.out.LoadNoticeImagePort;
import com.umc.product.notice.application.port.out.SaveNoticePort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileUsagePort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.support.IntegrationTestSupport;

@Import(NoticeFileUsageRollbackIntegrationTest.FailingUsageSaveConfig.class)
@DisplayName("공지 이미지 usage 저장 실패 rollback 통합")
class NoticeFileUsageRollbackIntegrationTest extends IntegrationTestSupport {

    private static final Long AUTHOR_ID = 7L;

    @Autowired
    private ManageNoticeContentUseCase manageNoticeContentUseCase;

    @Autowired
    private SaveNoticePort saveNoticePort;

    @Autowired
    private LoadNoticeImagePort loadNoticeImagePort;

    @Autowired
    private SaveFileMetadataPort saveFileMetadataPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void usage_row_저장이_실패하면_NoticeImage와_owner_anchor도_함께_rollback된다() {
        // given
        Notice notice = saveNoticePort.save(Notice.create("제목", "내용", AUTHOR_ID, false, false));
        saveConfirmedFile("notice-file");

        // when & then
        assertThatThrownBy(() -> manageNoticeContentUseCase.addImages(
            new AddNoticeImagesCommand(List.of("notice-file")), notice.getId(), AUTHOR_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("usage save failed");
        assertThat(loadNoticeImagePort.findImagesByNoticeId(notice.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_usage", Long.class))
            .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_usage_owner", Long.class))
            .isZero();
    }

    private void saveConfirmedFile(String fileId) {
        FileMetadata metadata = FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileId + ".png")
            .category(FileCategory.NOTICE_ATTACHMENT)
            .contentType("image/png")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("notice-rollback/" + fileId + ".png")
            .uploadedMemberId(AUTHOR_ID)
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
