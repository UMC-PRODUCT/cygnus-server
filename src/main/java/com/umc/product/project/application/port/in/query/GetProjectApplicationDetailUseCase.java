package com.umc.product.project.application.port.in.query;

import java.util.Collection;
import java.util.Map;

import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;

/**
 * 지원서 단건 상세 조회 UseCase.
 * <p>
 * 메타(지원자/매칭 차수/상태/시각) + 지원자 파트 기준 폼 구조 + 제출된 답변 + 첨부 파일 메타까지 한 번에 조립한다.
 * <p>
 * 권한 검사는 Controller 의 {@code @CheckAccess} 에서 처리한다. 지원자 본인 응답은 결과 공개 시점에 따라 status 가 {@code null} 일 수 있다.
 */
public interface GetProjectApplicationDetailUseCase {

    /**
     * 지원서 단건 상세를 조회한다. application 미존재 또는 정합성 위반(application 의 form.project.id ≠ projectId) 시
     * PROJECT_APPLICATION_NOT_FOUND 예외를 던진다.
     */
    ProjectApplicationDetailInfo getDetail(GetProjectApplicationDetailQuery query);

    /**
     * 지원서 상세를 여러 건 조회한다.
     * <p>
     * 모든 query.applicationId 는 존재해야 하며, 각 application 의 projectId 정합성도 단건 조회와 동일하게 검증한다.
     *
     * @return applicationId -> 상세 정보
     */
    Map<Long, ProjectApplicationDetailInfo> batchGetDetails(Collection<GetProjectApplicationDetailQuery> queries);
}
