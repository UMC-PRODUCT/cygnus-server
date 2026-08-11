package com.umc.product.global.graphql.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NodeGraphQlController — node/nodes 루트 필드")
class NodeGraphQlControllerTest {

    private record TestNode(String id) implements RelayNode {
    }

    @Mock
    NodeFetcher memberNodeFetcher;
    @Mock
    NodeFetcher gisuNodeFetcher;

    @Test
    void 등록된_타입의_전역_ID면_해당_NodeFetcher로_raw_ID_조회를_위임한다() {
        // given
        TestNode memberNode = new TestNode(GlobalId.encode(GlobalIdTypes.MEMBER, 42L));
        given(memberNodeFetcher.typeName()).willReturn(GlobalIdTypes.MEMBER);
        given(memberNodeFetcher.fetchOrNull(42L)).willReturn(memberNode);
        NodeGraphQlController controller = new NodeGraphQlController(List.of(memberNodeFetcher));

        // when
        RelayNode result = controller.node(GlobalId.encode(GlobalIdTypes.MEMBER, 42L));

        // then
        assertThat(result).isEqualTo(memberNode);
        assertThat(result.id()).isEqualTo(GlobalId.encode(GlobalIdTypes.MEMBER, 42L));
        then(memberNodeFetcher).should().fetchOrNull(42L);
    }

    @Test
    void 등록된_타입이어도_대상이_없으면_null을_반환한다() {
        // given
        given(memberNodeFetcher.typeName()).willReturn(GlobalIdTypes.MEMBER);
        given(memberNodeFetcher.fetchOrNull(999L)).willReturn(null);
        NodeGraphQlController controller = new NodeGraphQlController(List.of(memberNodeFetcher));

        // when
        RelayNode result = controller.node(GlobalId.encode(GlobalIdTypes.MEMBER, 999L));

        // then
        assertThat(result).isNull();
    }

    @Test
    void 미등록_타입의_전역_ID면_조회_없이_null을_반환한다() {
        // given — Gisu용 NodeFetcher는 등록되지 않았다.
        given(memberNodeFetcher.typeName()).willReturn(GlobalIdTypes.MEMBER);
        NodeGraphQlController controller = new NodeGraphQlController(List.of(memberNodeFetcher));

        // when
        RelayNode result = controller.node(GlobalId.encode(GlobalIdTypes.GISU, 1L));

        // then
        assertThat(result).isNull();
        then(memberNodeFetcher).should(never()).fetchOrNull(anyLong());
    }

    @Test
    void 전역_ID_형식이_아니면_예외가_발생한다() {
        // given
        given(memberNodeFetcher.typeName()).willReturn(GlobalIdTypes.MEMBER);
        NodeGraphQlController controller = new NodeGraphQlController(List.of(memberNodeFetcher));

        // when & then
        assertThatThrownBy(() -> controller.node("!!!not-a-global-id!!!"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nodes는_각_ID를_타입별_NodeFetcher로_매핑하고_순서를_보존한다() {
        // given
        TestNode memberNode1 = new TestNode(GlobalId.encode(GlobalIdTypes.MEMBER, 1L));
        TestNode memberNode3 = new TestNode(GlobalId.encode(GlobalIdTypes.MEMBER, 3L));
        TestNode gisuNode2 = new TestNode(GlobalId.encode(GlobalIdTypes.GISU, 2L));
        given(memberNodeFetcher.typeName()).willReturn(GlobalIdTypes.MEMBER);
        given(gisuNodeFetcher.typeName()).willReturn(GlobalIdTypes.GISU);
        given(memberNodeFetcher.fetchOrNull(1L)).willReturn(memberNode1);
        given(memberNodeFetcher.fetchOrNull(3L)).willReturn(memberNode3);
        given(gisuNodeFetcher.fetchOrNull(2L)).willReturn(gisuNode2);
        NodeGraphQlController controller = new NodeGraphQlController(
            List.of(memberNodeFetcher, gisuNodeFetcher)
        );

        // when
        List<RelayNode> results = controller.nodes(List.of(
            GlobalId.encode(GlobalIdTypes.MEMBER, 1L),
            GlobalId.encode(GlobalIdTypes.GISU, 2L),
            GlobalId.encode(GlobalIdTypes.MEMBER, 3L)
        ));

        // then
        assertThat(results).containsExactly(memberNode1, gisuNode2, memberNode3);
    }

    @Test
    void nodes는_미등록_타입이나_없는_대상을_null_원소로_반환한다() {
        // given — School용 NodeFetcher는 등록되지 않았고, Member 2는 존재하지 않는다.
        TestNode memberNode1 = new TestNode(GlobalId.encode(GlobalIdTypes.MEMBER, 1L));
        given(memberNodeFetcher.typeName()).willReturn(GlobalIdTypes.MEMBER);
        given(memberNodeFetcher.fetchOrNull(1L)).willReturn(memberNode1);
        given(memberNodeFetcher.fetchOrNull(2L)).willReturn(null);
        NodeGraphQlController controller = new NodeGraphQlController(List.of(memberNodeFetcher));

        // when
        List<RelayNode> results = controller.nodes(List.of(
            GlobalId.encode(GlobalIdTypes.MEMBER, 1L),
            GlobalId.encode(GlobalIdTypes.MEMBER, 2L),
            GlobalId.encode(GlobalIdTypes.SCHOOL, 3L)
        ));

        // then
        assertThat(results).containsExactly(memberNode1, null, null);
    }

    @Test
    void nodes에_빈_목록을_넘기면_빈_목록을_반환한다() {
        // given
        given(memberNodeFetcher.typeName()).willReturn(GlobalIdTypes.MEMBER);
        NodeGraphQlController controller = new NodeGraphQlController(List.of(memberNodeFetcher));

        // when
        List<RelayNode> results = controller.nodes(List.of());

        // then
        assertThat(results).isEmpty();
    }
}
