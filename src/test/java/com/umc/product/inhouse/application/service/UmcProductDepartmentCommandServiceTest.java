package com.umc.product.inhouse.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductDepartmentCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductDepartmentCommand;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductDepartmentPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductDepartment;
import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.exception.InhouseErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT Department 명령 서비스")
class UmcProductDepartmentCommandServiceTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Mock
    LoadUmcProductDepartmentPort loadUmcProductDepartmentPort;
    @Mock
    SaveUmcProductDepartmentPort saveUmcProductDepartmentPort;
    @Mock
    LoadUmcProductMemberPort loadUmcProductMemberPort;
    @Mock
    LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    @Mock
    LoadUmcProductDepartmentParticipantPort loadUmcProductDepartmentParticipantPort;
    @Mock
    SaveUmcProductDepartmentParticipantPort saveUmcProductDepartmentParticipantPort;
    @Mock
    UmcProductAccessPolicy umcProductAccessPolicy;

    @InjectMocks
    UmcProductDepartmentCommandService sut;

    @Test
    void 이미_존재하는_코드의_Department를_생성할_수_없다() {
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(true);
        given(loadUmcProductDepartmentPort.existsByCode("DUPLICATED", null)).willReturn(true);

        assertThatThrownBy(() -> sut.create(CreateUmcProductDepartmentCommand.of(
            100L,
            " DUPLICATED ",
            "중복 Department",
            null,
            START_DATE,
            END_DATE,
            1,
            true
        )))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_ALREADY_EXISTS);

        then(saveUmcProductDepartmentPort).shouldHaveNoInteractions();
    }

    @Test
    void 참가_이력이_수정된_Department_기간을_벗어나면_기간을_축소할_수_없다() {
        UmcProductDepartment department = department(1L);
        UmcProductDepartmentParticipant participant = org.mockito.Mockito.mock(
            UmcProductDepartmentParticipant.class
        );
        given(participant.getStartDate()).willReturn(LocalDate.of(2026, 1, 15));
        given(participant.getEndDate()).willReturn(LocalDate.of(2026, 6, 30));
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(true);
        given(loadUmcProductDepartmentPort.getByIdWithLock(1L)).willReturn(department);
        given(loadUmcProductDepartmentParticipantPort.listByDepartmentId(1L)).willReturn(List.of(participant));

        assertThatThrownBy(() -> sut.update(UpdateUmcProductDepartmentCommand.of(
            1L,
            100L,
            null,
            null,
            null,
            LocalDate.of(2026, 2, 1),
            END_DATE,
            null,
            null
        )))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);

        then(saveUmcProductDepartmentPort).should(never()).save(any());
    }

    @Test
    void 참가_이력이_있는_Department는_삭제할_수_없다() {
        UmcProductDepartment department = department(1L);
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(true);
        given(loadUmcProductDepartmentPort.getByIdWithLock(1L)).willReturn(department);
        given(loadUmcProductDepartmentParticipantPort.existsByDepartmentId(1L)).willReturn(true);

        assertThatThrownBy(() -> sut.delete(1L, 100L))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_HAS_PARTICIPANTS);

        then(saveUmcProductDepartmentPort).should(never()).delete(any());
    }

    private UmcProductDepartment department(Long id) {
        UmcProductDepartment department = UmcProductDepartment.create(
            "RECRUIT",
            "모집 Department",
            "기존 설명",
            START_DATE,
            END_DATE,
            1,
            true
        );
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }
}
