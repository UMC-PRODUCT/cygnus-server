package com.umc.product.organization.application.port.service.command;

import static com.umc.product.support.fixture.OrganizationUnitFixture.기수;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.out.command.SaveChapterSchoolPort;
import com.umc.product.organization.application.port.out.command.SaveGisuPort;
import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.exception.OrganizationDomainException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Organization command service 잔여 경로")
class OrganizationCommandServiceResidualTest {

    @Mock
    LoadGisuPort loadGisuPort;

    @Mock
    SaveGisuPort saveGisuPort;

    @Mock
    LoadChapterPort loadChapterPort;

    @Mock
    LoadSchoolPort loadSchoolPort;

    @Mock
    SaveSchoolPort saveSchoolPort;

    @Mock
    SaveChapterSchoolPort saveChapterSchoolPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    EvictAuthoritySnapshotCacheUseCase evictCacheUseCase;

    @Test
    @DisplayName("소속 지부가 있는 기수는 삭제할 수 없고 이미 활성인 기수는 갱신하지 않는다")
    void 기수_삭제와_활성화의_경계를_처리한다() {
        var service = new GisuService(loadGisuPort, saveGisuPort, loadChapterPort);
        var gisu = 기수(1L, 9L, true);
        given(loadGisuPort.getById(1L)).willReturn(gisu);
        given(loadChapterPort.existsByGisuId(1L)).willReturn(true);

        assertThatThrownBy(() -> service.deleteGisu(1L))
            .isInstanceOf(OrganizationDomainException.class);

        given(loadGisuPort.findActiveGisuWithLock()).willReturn(Optional.of(gisu));
        service.updateActiveGisu(1L);

        then(saveGisuPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("학교 일괄 삭제는 빈 입력을 무시하고 null ID만 있으면 회원 조회를 생략한다")
    void 학교_삭제의_빈_입력을_처리한다() {
        var service = new SchoolService(
            loadChapterPort, loadSchoolPort, saveSchoolPort, saveChapterSchoolPort,
            getMemberUseCase, evictCacheUseCase
        );

        service.deleteSchools(null);
        service.deleteSchools(List.of());
        service.deleteSchools(Arrays.asList(null, null));

        then(getMemberUseCase).shouldHaveNoInteractions();
        then(evictCacheUseCase).should().evictByMemberIds(Set.of());
    }
}
