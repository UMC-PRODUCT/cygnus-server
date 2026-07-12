package com.umc.product.storage.application.port.in.query;

import java.util.List;
import java.util.Map;

import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.application.port.in.query.dto.FileMetadataInfo;

/**
 * 파일 조회 UseCase
 */
public interface GetFileUseCase {
    /**
     * 내부용 인터페이스 입니다. 절대 API 단에 직접 조회할 수 있도록 하지 마세요.
     * <p>
     * fileId를 받아서, 파일의 정보를 반환하는 method 입니다.
     * <p>
     * 해당 과정을 통해서 사용자에게 응답 시에 fileId를 제공하는 것이 아닌 파일에 직접 접근할 수 있는 URL을 제공합니다.
     *
     * @param fileId 파일 ID
     * @return 파일 정보
     */
    FileInfo getById(String fileId);

    /**
     * 파일 ID 목록으로 파일 접근 URL을 일괄 조회합니다.
     * <p>
     * DB 조회를 IN 쿼리 1회로 수행하여 N+1 문제를 방지합니다.
     *
     * @param fileIds 파일 ID 목록
     * @return fileId → fileLink 매핑
     */
    Map<String, String> getFileLinks(List<String> fileIds);

    /**
     * 파일 ID 목록으로 파일 메타(원본 파일명/contentType/size 등)와 접근 URL을 일괄 조회합니다.
     * <p>
     * DB 조회를 IN 쿼리 1회로 수행하여 N+1 문제를 방지합니다. 누락된 fileId 는 결과 Map 에서 제외되므로(SaveAll 시맨틱이 아닌 graceful lookup), 호출자는 누락 가능성을
     * 가정해야 합니다.
     *
     * @param fileIds 파일 ID 목록
     * @return fileId -> {@link FileInfo} 매핑 (누락 fileId 는 빠짐)
     */
    Map<String, FileInfo> findAllByIds(List<String> fileIds);

    /**
     * 요청한 파일을 일괄 조회하고 해당 회원이 사용할 수 있는 업로드 완료 파일인지 검증합니다.
     * <p>
     * 접근 URL을 생성하지 않으며, 요청한 파일 중 하나라도 없거나 사용할 수 없으면 예외가 발생합니다.
     *
     * @param fileIds  파일 ID 목록
     * @param memberId 파일을 사용하려는 회원 ID
     * @return 검증이 완료된 파일 메타데이터 목록
     */
    List<FileMetadataInfo> batchGetUsableByIds(List<String> fileIds, Long memberId);

    /**
     * 파일이 존재하는지 확인합니다. Helper method 입니다.
     *
     * @param fileId 파일 ID
     * @return 존재 여부
     */
    boolean existsById(String fileId);

    void throwIfNotExists(String fileId);
}
