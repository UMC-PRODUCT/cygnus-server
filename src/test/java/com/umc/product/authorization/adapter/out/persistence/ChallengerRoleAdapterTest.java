package com.umc.product.authorization.adapter.out.persistence;

import static com.umc.product.support.fixture.AuthorizationFixture.중앙_역할;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRole persistence adapter")
class ChallengerRoleAdapterTest {

    @Mock
    ChallengerRoleJpaRepository jpaRepository;

    @Mock
    ChallengerRoleQueryRepository queryRepository;

    ChallengerRoleAdapter sut;

    @BeforeEach
    void setUp() {
        sut = new ChallengerRoleAdapter(jpaRepository, queryRepository);
    }

    @Test
    @DisplayName("member와 gisu 조회를 QueryDSL repository에 위임한다")
    void query_repository에_조회를_위임한다() {
        ChallengerRole role = 중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, 3L);
        given(queryRepository.findByMemberId(1L)).willReturn(List.of(role));
        given(queryRepository.findByMemberIdAndGisuId(1L, 3L)).willReturn(List.of(role));

        assertThat(sut.findByMemberId(1L)).containsExactly(role);
        assertThat(sut.findRolesByMemberIdAndGisuId(1L, 3L)).containsExactly(role);
    }

    @Test
    @DisplayName("ID·challenger ID 집합 조회를 JPA repository에 위임한다")
    void jpa_repository에_조회를_위임한다() {
        ChallengerRole role = 중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, 3L);
        given(jpaRepository.findByChallengerIdIn(Set.of(10L))).willReturn(List.of(role));
        given(jpaRepository.findById(1L)).willReturn(Optional.of(role));

        assertThat(sut.findByChallengerIdIn(Set.of(10L))).containsExactly(role);
        assertThat(sut.findById(1L)).contains(role);
        assertThat(sut.getById(1L)).isSameAs(role);
    }

    @Test
    @DisplayName("필수 get에서 ID가 없으면 CHALLENGER_ROLE_NOT_FOUND를 던진다")
    void 필수_get의_not_found를_구분한다() {
        given(jpaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getById(99L))
            .isInstanceOfSatisfying(AuthorizationDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(AuthorizationErrorCode.CHALLENGER_ROLE_NOT_FOUND)
            );
    }

    @Test
    @DisplayName("단건·대량 저장과 삭제를 JPA repository에 위임한다")
    void 저장과_삭제를_위임한다() {
        ChallengerRole role = 중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, 3L);
        given(jpaRepository.save(role)).willReturn(role);
        given(jpaRepository.saveAll(List.of(role))).willReturn(List.of(role));

        assertThat(sut.save(role)).isSameAs(role);
        assertThat(sut.saveAll(List.of(role))).containsExactly(role);
        sut.delete(role);

        then(jpaRepository).should().delete(role);
    }
}
