package com.umc.product.community.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.community.application.port.in.query.GetCommentListUseCase;
import com.umc.product.community.application.port.in.query.GetPostDetailUseCase;
import com.umc.product.community.application.port.in.query.dto.CommentInfo;
import com.umc.product.community.application.port.in.query.dto.PostInfo;

@ExtendWith(MockitoExtension.class)
class CommunityPermissionEvaluatorTest {

    @Mock
    GetPostDetailUseCase getPostDetailUseCase;

    @Mock
    GetCommentListUseCase getCommentListUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    CommunityPostPermissionEvaluator postEvaluator;
    CommunityCommentPermissionEvaluator commentEvaluator;

    @BeforeEach
    void setUp() {
        postEvaluator = new CommunityPostPermissionEvaluator(getPostDetailUseCase, getChallengerUseCase);
        commentEvaluator = new CommunityCommentPermissionEvaluator(getChallengerUseCase, getCommentListUseCase);
    }

    @Test
    @DisplayName("community evaluator는 각 resource type을 지원한다")
    void community_evaluator는_각_resource_type을_지원한다() {
        assertThat(postEvaluator.supportedResourceType()).isEqualTo(ResourceType.COMMUNITY_POST);
        assertThat(commentEvaluator.supportedResourceType()).isEqualTo(ResourceType.COMMUNITY_COMMENT);
    }

    @Test
    @DisplayName("게시글 READ는 허용하고 EDIT는 작성자 회원에게만 허용한다")
    void 게시글_READ는_허용하고_EDIT는_작성자_회원에게만_허용한다() {
        givenPostAuthor();

        assertThat(postEvaluator.evaluate(subject(100L), postPermission(PermissionType.READ))).isTrue();
        assertThat(postEvaluator.evaluate(subject(100L), postPermission(PermissionType.EDIT))).isTrue();
        assertThat(postEvaluator.evaluate(subject(200L), postPermission(PermissionType.EDIT))).isFalse();
    }

    @Test
    @DisplayName("게시글 WRITE는 리소스 작성자가 아닌 요청자의 challenger 이력을 검사한다")
    void 게시글_WRITE는_요청자의_challenger_이력을_검사한다() {
        givenPostAuthor();
        given(getChallengerUseCase.getAllByMemberId(200L)).willReturn(List.of());

        assertThat(postEvaluator.evaluate(subject(200L), postPermission(PermissionType.WRITE))).isFalse();

        given(getChallengerUseCase.getAllByMemberId(300L)).willReturn(List.of(challenger(2L, 300L)));
        assertThat(postEvaluator.evaluate(subject(300L), postPermission(PermissionType.WRITE))).isTrue();
    }

    @Test
    @DisplayName("게시글 DELETE는 작성자 또는 중앙 총괄단에게만 허용한다")
    void 게시글_DELETE는_작성자_또는_중앙_총괄단에게만_허용한다() {
        givenPostAuthor();

        assertThat(postEvaluator.evaluate(subject(200L), postPermission(PermissionType.DELETE))).isFalse();
        assertThat(postEvaluator.evaluate(centralSubject(200L), postPermission(PermissionType.DELETE))).isTrue();
        assertThat(postEvaluator.evaluate(subject(100L), postPermission(PermissionType.DELETE))).isTrue();
    }

    @Test
    @DisplayName("댓글 READ는 허용하고 EDIT는 작성자 회원에게만 허용한다")
    void 댓글_READ는_허용하고_EDIT는_작성자_회원에게만_허용한다() {
        givenCommentAuthor();

        assertThat(commentEvaluator.evaluate(subject(100L), commentPermission(PermissionType.READ))).isTrue();
        assertThat(commentEvaluator.evaluate(subject(100L), commentPermission(PermissionType.EDIT))).isTrue();
        assertThat(commentEvaluator.evaluate(subject(200L), commentPermission(PermissionType.EDIT))).isFalse();
    }

    @Test
    @DisplayName("댓글 WRITE는 리소스 작성자가 아닌 요청자의 challenger 이력을 검사한다")
    void 댓글_WRITE는_요청자의_challenger_이력을_검사한다() {
        givenCommentAuthor();
        given(getChallengerUseCase.getAllByMemberId(200L)).willReturn(List.of());

        assertThat(commentEvaluator.evaluate(subject(200L), commentPermission(PermissionType.WRITE))).isFalse();

        given(getChallengerUseCase.getAllByMemberId(300L)).willReturn(List.of(challenger(2L, 300L)));
        assertThat(commentEvaluator.evaluate(subject(300L), commentPermission(PermissionType.WRITE))).isTrue();
    }

    @Test
    @DisplayName("댓글 DELETE는 작성자 또는 중앙 총괄단에게만 허용한다")
    void 댓글_DELETE는_작성자_또는_중앙_총괄단에게만_허용한다() {
        givenCommentAuthor();

        assertThat(commentEvaluator.evaluate(subject(200L), commentPermission(PermissionType.DELETE))).isFalse();
        assertThat(commentEvaluator.evaluate(centralSubject(200L), commentPermission(PermissionType.DELETE))).isTrue();
        assertThat(commentEvaluator.evaluate(subject(100L), commentPermission(PermissionType.DELETE))).isTrue();
    }

    @Test
    @DisplayName("지원하지 않는 permission은 fail-closed로 거부한다")
    void 지원하지_않는_permission은_fail_closed로_거부한다() {
        givenPostAuthor();
        givenCommentAuthor();
        ResourcePermission post = mock(ResourcePermission.class);
        ResourcePermission comment = mock(ResourcePermission.class);
        given(post.getResourceIdAsLong()).willReturn(10L);
        given(post.permission()).willReturn(PermissionType.APPROVE);
        given(comment.getResourceIdAsLong()).willReturn(20L);
        given(comment.permission()).willReturn(PermissionType.APPROVE);

        assertThat(postEvaluator.evaluate(subject(100L), post)).isFalse();
        assertThat(commentEvaluator.evaluate(subject(100L), comment)).isFalse();
    }

    private void givenPostAuthor() {
        given(getPostDetailUseCase.getPostDetail(10L)).willReturn(PostInfo.builder()
            .postId(10L)
            .authorChallengerId(1L)
            .build());
        given(getChallengerUseCase.getById(1L)).willReturn(challenger(1L, 100L));
    }

    private void givenCommentAuthor() {
        given(getCommentListUseCase.getComment(20L)).willReturn(CommentInfo.builder()
            .commentId(20L)
            .challengerId(1L)
            .build());
        given(getChallengerUseCase.getById(1L)).willReturn(challenger(1L, 100L));
    }

    private ChallengerInfo challenger(Long challengerId, Long memberId) {
        return ChallengerInfo.builder().challengerId(challengerId).memberId(memberId).build();
    }

    private SubjectAttributes subject(Long memberId) {
        return SubjectAttributes.builder().memberId(memberId).build();
    }

    private SubjectAttributes centralSubject(Long memberId) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .roleAttributes(List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                null,
                10L
            )))
            .build();
    }

    private ResourcePermission postPermission(PermissionType permission) {
        return ResourcePermission.of(ResourceType.COMMUNITY_POST, 10L, permission);
    }

    private ResourcePermission commentPermission(PermissionType permission) {
        return ResourcePermission.of(ResourceType.COMMUNITY_COMMENT, 20L, permission);
    }
}
