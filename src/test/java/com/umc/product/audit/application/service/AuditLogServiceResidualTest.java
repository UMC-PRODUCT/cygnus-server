package com.umc.product.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.audit.application.port.in.query.dto.AuditLogInfo;
import com.umc.product.audit.application.port.in.query.dto.SearchAuditLogQuery;
import com.umc.product.audit.application.port.out.LoadAuditLogPort;
import com.umc.product.audit.application.port.out.SaveAuditLogPort;
import com.umc.product.audit.application.service.command.AuditLogCommandService;
import com.umc.product.audit.application.service.query.AuditLogQueryService;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.Domain;

@DisplayName("감사 로그 application 잔여 계약")
class AuditLogServiceResidualTest {

    @Test
    @DisplayName("상세 정보가 있으면 JSON으로 직렬화해 로그를 저장한다")
    void 상세_정보를_직렬화해_저장한다() throws Exception {
        SaveAuditLogPort savePort = mock(SaveAuditLogPort.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AuditLogCommandService sut = new AuditLogCommandService(savePort, objectMapper);
        AuditLogEvent event = event(Map.of("before", "a", "after", "b"));
        given(objectMapper.writeValueAsString(event.details())).willReturn("{\"before\":\"a\"}");

        sut.save(event);

        then(savePort).should().save(any(AuditLog.class));
        then(objectMapper).should().writeValueAsString(event.details());
    }

    @Test
    @DisplayName("상세 정보가 null이거나 비어 있으면 직렬화 없이 저장한다")
    void 빈_상세_정보는_직렬화하지_않는다() {
        SaveAuditLogPort savePort = mock(SaveAuditLogPort.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AuditLogCommandService sut = new AuditLogCommandService(savePort, objectMapper);

        sut.save(event(null));
        sut.save(event(Map.of()));

        then(objectMapper).shouldHaveNoInteractions();
        then(savePort).should(times(2)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("상세 정보 직렬화가 실패해도 본 업무와 로그 저장을 중단하지 않는다")
    void 직렬화_실패는_null_상세로_격리한다() throws Exception {
        SaveAuditLogPort savePort = mock(SaveAuditLogPort.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AuditLogCommandService sut = new AuditLogCommandService(savePort, objectMapper);
        AuditLogEvent event = event(Map.of("invalid", new Object()));
        given(objectMapper.writeValueAsString(event.details()))
            .willThrow(new JsonProcessingException("직렬화 실패") { });

        sut.save(event);

        then(savePort).should().save(any(AuditLog.class));
    }

    @Test
    @DisplayName("조회 조건과 페이지를 port에 그대로 전달하고 로그를 DTO로 변환한다")
    void 조회_조건을_전달하고_DTO로_변환한다() {
        LoadAuditLogPort loadPort = mock(LoadAuditLogPort.class);
        AuditLogQueryService sut = new AuditLogQueryService(loadPort);
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-31T23:59:59Z");
        SearchAuditLogQuery query = new SearchAuditLogQuery(Domain.MEMBER, AuditAction.UPDATE, 1L, from, to);
        PageRequest pageable = PageRequest.of(1, 20);
        AuditLog log = AuditLog.from(event(null), null, "127.0.0.1");
        given(loadPort.search(Domain.MEMBER, AuditAction.UPDATE, 1L, from, to, pageable))
            .willReturn(new PageImpl<>(List.of(log)));

        assertThat(sut.search(query, pageable).map(AuditLogInfo::domain).getContent())
            .containsExactly(Domain.MEMBER);
    }

    @Test
    @DisplayName("감사 로그 조회 권한은 중앙 운영진에게만 허용하고 미지원 권한은 fail-closed 처리한다")
    void 감사_로그_권한을_검증한다() {
        AuditLogPermissionEvaluator sut = new AuditLogPermissionEvaluator();
        ResourcePermission read = ResourcePermission.ofType(ResourceType.AUDIT, PermissionType.READ);
        SubjectAttributes central = SubjectAttributes.builder()
            .memberId(1L)
            .roleAttributes(List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, null, 9L)))
            .build();
        SubjectAttributes normal = SubjectAttributes.builder().memberId(2L).build();
        ResourcePermission unsupported = mock(ResourcePermission.class);
        given(unsupported.permission()).willReturn(PermissionType.WRITE);

        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.AUDIT);
        assertThat(sut.evaluate(central, read)).isTrue();
        assertThat(sut.evaluate(normal, read)).isFalse();
        assertThatThrownBy(() -> sut.evaluate(normal, unsupported)).isInstanceOf(CommonException.class);
    }

    private AuditLogEvent event(Map<String, Object> details) {
        return AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.UPDATE)
            .targetType("Member")
            .targetId("10")
            .actorMemberId(1L)
            .description("회원 수정")
            .details(details)
            .ipAddress("127.0.0.1")
            .build();
    }
}
