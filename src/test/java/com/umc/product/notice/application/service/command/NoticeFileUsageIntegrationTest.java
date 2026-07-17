package com.umc.product.notice.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.notice.application.port.in.command.ManageNoticeContentUseCase;
import com.umc.product.notice.application.port.in.command.ManageNoticeUseCase;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.DeleteNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.ReplaceNoticeImagesCommand;
import com.umc.product.notice.application.port.out.LoadNoticeImagePort;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.SaveNoticePort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("공지 이미지 FileUsageRegistry 통합")
class NoticeFileUsageIntegrationTest extends IntegrationTestSupport {

    private static final Long AUTHOR_ID = 7L;
    private static final Instant CONFIRMED_AT = Instant.parse("2026-07-17T00:00:00Z");

    @Autowired
    private ManageNoticeContentUseCase manageNoticeContentUseCase;

    @Autowired
    private ManageNoticeUseCase manageNoticeUseCase;

    @Autowired
    private SaveNoticePort saveNoticePort;

    @Autowired
    private LoadNoticePort loadNoticePort;

    @Autowired
    private LoadNoticeImagePort loadNoticeImagePort;

    @Autowired
    private ManageFileUsageUseCase manageFileUsageUseCase;

    @Autowired
    private SaveFileMetadataPort saveFileMetadataPort;

    @Autowired
    private LoadFileMetadataPort loadFileMetadataPort;

    @Autowired
    private LoadFileUsagePort loadFileUsagePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 이미지_A_B를_B_C로_교체하면_exact_snapshot과_shared_file을_보존한다() {
        // given
        Notice notice = saveNotice();
        saveConfirmedFile("file-a", AUTHOR_ID);
        saveConfirmedFile("file-b", AUTHOR_ID);
        saveConfirmedFile("file-c", AUTHOR_ID);

        manageNoticeContentUseCase.addImages(
            new AddNoticeImagesCommand(List.of("file-a", "file-b")), notice.getId(), AUTHOR_ID);
        FileUsageCoordinate sharedOwner = FileUsageCoordinate.of("project", "999", "logo");
        manageFileUsageUseCase.replaceUsages(
            new ReplaceFileUsagesCommand(sharedOwner, Set.of("file-a"), AUTHOR_ID));

        // when
        manageNoticeContentUseCase.replaceImages(
            new ReplaceNoticeImagesCommand(List.of("file-b", "file-c")), notice.getId(), AUTHOR_ID);

        // then
        assertThat(usageSnapshot(noticeImages(notice.getId())))
            .containsExactlyInAnyOrder("file-b", "file-c");
        assertThat(loadFileUsagePort.countByFileId("file-a")).isEqualTo(1L);
        assertThat(loadFileMetadataPort.findByFileId("file-a").orElseThrow().getUnreferencedAt())
            .isNull();
    }

    @Test
    void 공지_hard_delete는_이미지_usage를_먼저_detach한다() {
        // given
        Notice notice = saveNotice();
        saveConfirmedFile("delete-file", AUTHOR_ID);
        manageNoticeContentUseCase.addImages(
            new AddNoticeImagesCommand(List.of("delete-file")), notice.getId(), AUTHOR_ID);

        // when
        manageNoticeUseCase.deleteNotice(new DeleteNoticeCommand(AUTHOR_ID, notice.getId()));

        // then
        assertThat(usageSnapshot(noticeImages(notice.getId()))).isEmpty();
        assertThat(loadNoticePort.findNoticeById(notice.getId())).isEmpty();
        assertThat(loadFileUsagePort.countByFileId("delete-file")).isZero();
        assertThat(loadFileMetadataPort.findByFileId("delete-file").orElseThrow().getUnreferencedAt())
            .isNotNull();
    }

    @Test
    void 다른_uploader의_이미지_attach가_실패하면_NoticeImage도_rollback된다() {
        // given
        Notice notice = saveNotice();
        saveConfirmedFile("other-file", 99L);

        // when & then
        assertThatThrownBy(() -> manageNoticeContentUseCase.addImages(
            new AddNoticeImagesCommand(List.of("other-file")), notice.getId(), AUTHOR_ID))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_USE_FORBIDDEN);
        assertThat(loadNoticeImagePort.findImagesByNoticeId(notice.getId())).isEmpty();
        assertThat(usageSnapshot(noticeImages(notice.getId()))).isEmpty();
    }

    private Notice saveNotice() {
        return saveNoticePort.save(Notice.create("제목", "내용", AUTHOR_ID, false, false));
    }

    private FileUsageCoordinate noticeImages(Long noticeId) {
        return FileUsageCoordinate.of("notice", noticeId.toString(), "images");
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

    private void saveConfirmedFile(String fileId, Long uploaderId) {
        FileMetadata metadata = FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileId + ".png")
            .category(FileCategory.ETC)
            .contentType("image/png")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("notice-test/" + fileId + ".png")
            .uploadedMemberId(uploaderId)
            .build();
        metadata.markAsUploaded(CONFIRMED_AT);
        saveFileMetadataPort.save(metadata);
    }
}
