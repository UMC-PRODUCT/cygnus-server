package com.umc.product.authorization.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ResourcePermissionInfo;
import com.umc.product.authorization.application.port.in.query.dto.ResourcePermissionQuery;
import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("리소스 권한 조회 입력 검증")
class CheckResourcePermissionValidationTest {

    private static final Long MEMBER_ID = 1L;
    private static final SubjectAttributes SUBJECT = SubjectAttributes.builder().memberId(MEMBER_ID).build();

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    @Mock
    ResourcePermissionEvaluator noticeEvaluator;

    CheckResourcePermissionService sut;

    @BeforeEach
    void setUp() {
        given(noticeEvaluator.supportedResourceType()).willReturn(ResourceType.NOTICE);
        sut = new CheckResourcePermissionService(checkPermissionUseCase, List.of(noticeEvaluator));
    }

    @Nested
    @DisplayName("배치 요청 구조")
    class BatchShape {

        @Test
        @DisplayName("null·empty query 목록은 subject를 로드하기 전에 거부한다")
        void 빈_query_목록을_거부한다() {
            assertInvalid(() -> sut.batchHasPermission(MEMBER_ID, null));
            assertInvalid(() -> sut.batchHasPermission(MEMBER_ID, List.of()));
            then(checkPermissionUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("query 요소 null은 subject를 로드하기 전에 거부한다")
        void null_query를_거부한다() {
            assertInvalid(() -> sut.batchHasPermission(MEMBER_ID, Arrays.asList((ResourcePermissionQuery) null)));
            then(checkPermissionUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("resource type null과 evaluator 미등록을 각각 입력 오류와 미지원 오류로 구분한다")
        void resource_type_오류를_구분한다() {
            assertInvalid(() -> sut.batchHasPermission(
                MEMBER_ID,
                List.of(ResourcePermissionQuery.of(null, List.of(1L), List.of(PermissionType.READ)))
            ));

            assertThatThrownBy(() -> sut.batchHasPermission(
                MEMBER_ID,
                List.of(ResourcePermissionQuery.of(
                    ResourceType.MEMBER,
                    List.of(1L),
                    List.of(PermissionType.READ)
                ))
            )).isInstanceOfSatisfying(AuthorizationDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(AuthorizationErrorCode.NO_EVALUATOR_MATCHING_RESOURCE_TYPE)
            );
        }
    }

    @Nested
    @DisplayName("배치 필드")
    class BatchFields {

        @Test
        @DisplayName("resourceIds는 null은 타입 단위로 허용하지만 empty와 null 요소는 거부한다")
        void resource_ids_계약을_검증한다() {
            assertInvalid(() -> sut.batchHasPermission(
                MEMBER_ID,
                List.of(ResourcePermissionQuery.of(ResourceType.NOTICE, List.of(), List.of(PermissionType.READ)))
            ));
            assertInvalid(() -> sut.batchHasPermission(
                MEMBER_ID,
                List.of(ResourcePermissionQuery.of(
                    ResourceType.NOTICE,
                    Arrays.asList(1L, null),
                    List.of(PermissionType.READ)
                ))
            ));
        }

        @Test
        @DisplayName("permissionTypes는 null은 전체 권한으로 허용하지만 empty와 null 요소는 거부한다")
        void permission_types_계약을_검증한다() {
            assertInvalid(() -> sut.batchHasPermission(
                MEMBER_ID,
                List.of(ResourcePermissionQuery.of(ResourceType.NOTICE, List.of(1L), List.of()))
            ));
            assertInvalid(() -> sut.batchHasPermission(
                MEMBER_ID,
                List.of(ResourcePermissionQuery.of(
                    ResourceType.NOTICE,
                    List.of(1L),
                    Arrays.asList(PermissionType.READ, null)
                ))
            ));
        }

        @Test
        @DisplayName("resourceIds와 permissionTypes가 둘 다 null이면 타입의 전체 지원 권한을 ordinal 순서로 평가한다")
        void null_필드는_전체_권한을_평가한다() {
            given(checkPermissionUseCase.loadSubject(MEMBER_ID)).willReturn(SUBJECT);
            given(checkPermissionUseCase.check(
                any(SubjectAttributes.class),
                any(ResourcePermission.class)
            )).willReturn(true);

            List<ResourcePermissionInfo> result = sut.batchHasPermission(
                MEMBER_ID,
                List.of(ResourcePermissionQuery.of(ResourceType.NOTICE, null, null))
            );

            assertThat(result).singleElement().satisfies(info -> {
                assertThat(info.resourceId()).isNull();
                assertThat(info.permissions().keySet())
                    .containsExactly(PermissionType.READ, PermissionType.EDIT, PermissionType.DELETE, PermissionType.CHECK);
            });
        }

        @Test
        @DisplayName("요청 permission 순서와 관계없이 enum ordinal 순서로 안정적으로 반환한다")
        void permission_순서를_정규화한다() {
            given(checkPermissionUseCase.loadSubject(MEMBER_ID)).willReturn(SUBJECT);
            given(checkPermissionUseCase.check(
                any(SubjectAttributes.class),
                any(ResourcePermission.class)
            )).willReturn(false);

            ResourcePermissionInfo result = sut.batchHasPermission(
                MEMBER_ID,
                List.of(ResourcePermissionQuery.of(
                    ResourceType.NOTICE,
                    List.of(1L),
                    List.of(PermissionType.CHECK, PermissionType.READ, PermissionType.DELETE)
                ))
            ).getFirst();

            assertThat(result.permissions().keySet())
                .containsExactly(PermissionType.READ, PermissionType.DELETE, PermissionType.CHECK);
        }
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(AuthorizationDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(AuthorizationErrorCode.INVALID_INPUT_VALUE)
            );
    }
}
