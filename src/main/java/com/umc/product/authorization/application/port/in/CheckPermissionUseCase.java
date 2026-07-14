package com.umc.product.authorization.application.port.in;

import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.SubjectAttributes;

/**
 * 특정 주체가 권한이 있는지를 평가합니다.
 * <p>
 * RBAC 및 ABAC를 모두 사용하도록 하며, 주체 속성이 특정 리소스에 대한 권한을 가지고 있는지를 평가합니다.
 * <p>
 * 추후 객체 속성 및 환경 속성 또한 추가하는 메소드를 오버로딩하여 구현할 수 있습니다.
 */
public interface CheckPermissionUseCase {

    /**
     * 사용자 권한 평가에 필요한 주체 속성을 조회
     *
     * @param memberId 사용자 ID
     * @return 권한 평가에 사용할 주체 속성
     */
    SubjectAttributes loadSubject(Long memberId);

    /**
     * 사용자가 특정 리소스에 대한 권한이 있는지 확인
     *
     * @param memberId   사용자 ID
     * @param permission 확인할 권한
     * @return 권한 있으면 true, 없으면 false
     */
    boolean check(Long memberId, ResourcePermission permission);

    boolean check(Long memberId, ResourcePermission permission, String actionId);

    /**
     * 이미 조회된 주체 속성으로 특정 리소스에 대한 권한이 있는지 확인
     *
     * @param subjectAttributes 권한 평가에 사용할 주체 속성
     * @param permission        확인할 권한
     * @return 권한 있으면 true, 없으면 false
     */
    boolean check(SubjectAttributes subjectAttributes, ResourcePermission permission);

    boolean check(SubjectAttributes subjectAttributes, ResourcePermission permission, String actionId);

    /**
     * 권한이 없으면 예외를 발생시키는 메서드
     *
     * @param memberId   사용자 ID
     * @param permission 확인할 권한
     * @throws com.umc.product.authorization.domain.exception.AuthorizationDomainException 권한이 없는 경우
     */
    void checkOrThrow(Long memberId, ResourcePermission permission);

    void checkOrThrow(Long memberId, ResourcePermission permission, String actionId);

    void checkOrThrow(SubjectAttributes subjectAttributes, ResourcePermission permission, String actionId);
}
