package com.umc.product.community.application.port.in.query.thread.message.dto;

import com.umc.product.storage.application.port.in.query.dto.FileInfo;

/**
 * 스레드 메시지에 첨부된 파일 하나의 조회 모델.
 *
 * <p>파일 ID와 접근 URL을 한 객체로 묶어, 클라이언트가 두 목록을 인덱스로 짝지을 필요를 없앤다.
 * storage 메타에서 누락된 파일은 목록에서 제외되므로 인덱스 기반 매칭은 어긋날 수 있다.</p>
 *
 * <p>{@code fileUrl} 은 유효기간이 있는 서명 URL 이며, 조회할 때마다 새로 발급된다.</p>
 */
public record CommunityThreadMessageFileInfo(
    String fileId,
    String fileName,
    Long fileSize,
    String fileUrl
) {

    public CommunityThreadMessageFileInfo {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("fileId must not be blank");
        }
    }

    public static CommunityThreadMessageFileInfo from(FileInfo file) {
        return new CommunityThreadMessageFileInfo(
            file.fileId(),
            file.originalFileName(),
            file.fileSize(),
            file.fileLink()
        );
    }
}
