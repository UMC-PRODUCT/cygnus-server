package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.demoday.application.port.in.command.dto.DemodayTestDataResetInfo;
import com.umc.product.demoday.application.port.out.DeleteDemodayTestDataPort;
import com.umc.product.demoday.application.port.out.dto.DemodayTestDataDeletionCounts;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.global.exception.constant.Domain;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResetDemodayTestDataCommandService")
class ResetDemodayTestDataCommandServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private DemodayAdminAccessChecker adminAccessChecker;

    @Mock
    private DeleteDemodayTestDataPort deleteDemodayTestDataPort;

    @InjectMocks
    private ResetDemodayTestDataCommandService service;

    @Test
    @DisplayName("SUPER_ADMIN 권한을 먼저 확인한 뒤 전체 테스트 데이터를 삭제하고 건수를 반환한다")
    void resetAfterSystemAdminAccessValidation() {
        // given
        DemodayTestDataDeletionCounts counts = new DemodayTestDataDeletionCounts(2, 3, 4, 5, 6);
        given(deleteDemodayTestDataPort.deleteAll()).willReturn(counts);

        // when
        DemodayTestDataResetInfo result = service.reset(MEMBER_ID);

        // then
        assertThat(result).isEqualTo(new DemodayTestDataResetInfo(2, 3, 4, 5, 6));

        InOrder invocationOrder = inOrder(adminAccessChecker, deleteDemodayTestDataPort);
        invocationOrder.verify(adminAccessChecker).validateSystemAdminAccess(MEMBER_ID);
        invocationOrder.verify(deleteDemodayTestDataPort).deleteAll();
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한이 없으면 테스트 데이터를 삭제하지 않는다")
    void rejectResetWhenSystemAdminAccessIsDenied() {
        // given
        DemodayDomainException expectedException =
            new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED);
        willThrow(expectedException)
            .given(adminAccessChecker)
            .validateSystemAdminAccess(MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> service.reset(MEMBER_ID))
            .isSameAs(expectedException);

        then(deleteDemodayTestDataPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성공한 초기화는 데모데이 DELETE 감사 로그 대상으로 선언한다")
    void declareAuditLogForSuccessfulReset() throws NoSuchMethodException {
        // given
        Method resetMethod = ResetDemodayTestDataCommandService.class.getMethod("reset", Long.class);

        // when
        Audited audited = resetMethod.getAnnotation(Audited.class);

        // then
        assertThat(audited).isNotNull();
        assertThat(audited.domain()).isEqualTo(Domain.DEMODAY);
        assertThat(audited.action()).isEqualTo(AuditAction.DELETE);
        assertThat(audited.targetType()).isEqualTo("DemodayTestData");
        assertThat(audited.description()).isNotBlank();
    }
}
