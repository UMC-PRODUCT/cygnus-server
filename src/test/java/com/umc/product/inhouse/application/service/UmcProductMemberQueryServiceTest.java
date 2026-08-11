package com.umc.product.inhouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberSearchCondition;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.application.port.out.query.dto.UmcProductMemberSearchCriteria;
import com.umc.product.inhouse.domain.UmcProductDepartment;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
class UmcProductMemberQueryServiceTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);

    @Mock
    LoadUmcProductMemberPort loadUmcProductMemberPort;
    @Mock
    LoadUmcProductMemberAccountPort loadUmcProductMemberAccountPort;
    @Mock
    LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    @Mock
    LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;
    @Mock
    LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    @Mock
    LoadUmcProductDepartmentParticipantPort loadUmcProductDepartmentParticipantPort;
    @Mock
    LoadUmcProductDepartmentPort loadUmcProductDepartmentPort;
    @Mock
    GetSchoolUseCase getSchoolUseCase;
    @Mock
    GetFileUseCase getFileUseCase;

    @InjectMocks
    UmcProductMemberQueryService sut;

    @Test
    void includeDescendants면_모든_하위_Department를_검색_조건에_포함한다() {
        UmcProductDepartment root = department(10L, null);
        UmcProductDepartment child = department(20L, root);
        UmcProductDepartment grandchild = department(30L, child);
        UmcProductDepartment otherRoot = department(40L, null);
        given(loadUmcProductDepartmentPort.listAll(null, null))
            .willReturn(List.of(root, child, grandchild, otherRoot));
        given(loadUmcProductMemberPort.searchIds(any(), any())).willReturn(Page.empty());

        sut.search(
            UmcProductMemberSearchCondition.of(null, null, null, 10L, true, null),
            PageRequest.of(0, 10)
        );

        ArgumentCaptor<UmcProductMemberSearchCriteria> captor = ArgumentCaptor.forClass(
            UmcProductMemberSearchCriteria.class
        );
        verify(loadUmcProductMemberPort).searchIds(captor.capture(), any());
        assertThat(captor.getValue().departmentIds()).containsExactlyInAnyOrder(10L, 20L, 30L);
    }

    @Test
    void 로그인_계정에_연동된_인원이_없으면_빈_프로필을_반환한다() {
        given(loadUmcProductMemberAccountPort.findByMemberId(100L)).willReturn(java.util.Optional.empty());

        assertThat(sut.findByAccountMemberId(100L)).isEmpty();

        verifyNoInteractions(loadUmcProductMemberPort);
    }

    private UmcProductDepartment department(Long id, UmcProductDepartment parent) {
        UmcProductDepartment department = UmcProductDepartment.create(
            "D" + id,
            "Department " + id,
            null,
            parent,
            START_DATE,
            null,
            id.intValue(),
            true
        );
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }
}
