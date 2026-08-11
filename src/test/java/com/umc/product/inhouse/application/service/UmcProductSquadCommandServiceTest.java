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
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductSquadCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductSquadCommand;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductSquadParticipantPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductSquadPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.inhouse.domain.UmcProductSquad;
import com.umc.product.inhouse.domain.UmcProductSquadParticipant;
import com.umc.product.inhouse.exception.InhouseErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT Squad 명령 서비스")
class UmcProductSquadCommandServiceTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Mock
    LoadUmcProductSquadPort loadUmcProductSquadPort;
    @Mock
    SaveUmcProductSquadPort saveUmcProductSquadPort;
    @Mock
    LoadUmcProductMemberPort loadUmcProductMemberPort;
    @Mock
    LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    @Mock
    LoadUmcProductSquadParticipantPort loadUmcProductSquadParticipantPort;
    @Mock
    SaveUmcProductSquadParticipantPort saveUmcProductSquadParticipantPort;
    @Mock
    UmcProductAccessPolicy umcProductAccessPolicy;

    @InjectMocks
    UmcProductSquadCommandService sut;

    @Test
    void 이미_존재하는_코드의_Squad를_생성할_수_없다() {
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(true);
        given(loadUmcProductSquadPort.existsByCode("DUPLICATED", null)).willReturn(true);

        assertThatThrownBy(() -> sut.create(CreateUmcProductSquadCommand.of(
            100L,
            " DUPLICATED ",
            "중복 Squad",
            null,
            START_DATE,
            END_DATE,
            1,
            true
        )))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_SQUAD_ALREADY_EXISTS);

        then(saveUmcProductSquadPort).shouldHaveNoInteractions();
    }

    @Test
    void 참가_이력이_수정된_Squad_기간을_벗어나면_기간을_축소할_수_없다() {
        UmcProductSquad squad = squad(1L);
        UmcProductSquadParticipant participant = org.mockito.Mockito.mock(
            UmcProductSquadParticipant.class
        );
        given(participant.getStartDate()).willReturn(LocalDate.of(2026, 1, 15));
        given(participant.getEndDate()).willReturn(LocalDate.of(2026, 6, 30));
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(true);
        given(loadUmcProductSquadPort.getByIdWithLock(1L)).willReturn(squad);
        given(loadUmcProductSquadParticipantPort.listBySquadId(1L)).willReturn(List.of(participant));

        assertThatThrownBy(() -> sut.update(UpdateUmcProductSquadCommand.of(
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

        then(saveUmcProductSquadPort).should(never()).save(any());
    }

    @Test
    void 참가_이력이_있는_Squad는_삭제할_수_없다() {
        UmcProductSquad squad = squad(1L);
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(true);
        given(loadUmcProductSquadPort.getByIdWithLock(1L)).willReturn(squad);
        given(loadUmcProductSquadParticipantPort.existsBySquadId(1L)).willReturn(true);

        assertThatThrownBy(() -> sut.delete(1L, 100L))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_SQUAD_HAS_PARTICIPANTS);

        then(saveUmcProductSquadPort).should(never()).delete(any());
    }

    private UmcProductSquad squad(Long id) {
        UmcProductSquad squad = UmcProductSquad.create(
            "RECRUIT",
            "모집 Squad",
            "기존 설명",
            START_DATE,
            END_DATE,
            1,
            true
        );
        ReflectionTestUtils.setField(squad, "id", id);
        return squad;
    }
}
