package com.umc.product.organization.adapter.out.persistence.umcproduct;

import static com.umc.product.support.fixture.OrganizationUnitFixture.리더십;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드_참여;
import static com.umc.product.support.fixture.OrganizationUnitFixture.챕터_소속;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_멤버;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_챕터;
import static com.umc.product.support.fixture.OrganizationUnitFixture.활동_기간;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT persistence adapter 잔여 경로")
class UmcProductPersistenceAdapterResidualTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Nested
    @DisplayName("프로덕트 멤버")
    class MemberAdapter {
        @Mock
        UmcProductMemberJpaRepository jpaRepository;
        @Mock
        UmcProductMemberQueryRepository queryRepository;

        @Test
        @DisplayName("ID와 회원 ID 조회·잠금·목록·검색·삭제 및 not-found를 처리한다")
        void 멤버_adapter_계약을_보장한다() {
            var member = 프로덕트_멤버(1L, 10L, null);
            var adapter = new UmcProductMemberPersistenceAdapter(jpaRepository, queryRepository);
            given(jpaRepository.findById(1L)).willReturn(Optional.of(member));
            given(jpaRepository.findById(9L)).willReturn(Optional.empty());
            given(jpaRepository.findByIdWithLock(1L)).willReturn(Optional.of(member));
            given(jpaRepository.findByIdWithLock(9L)).willReturn(Optional.empty());
            given(jpaRepository.findByMemberId(10L)).willReturn(Optional.of(member));
            given(jpaRepository.findByMemberId(90L)).willReturn(Optional.empty());
            given(jpaRepository.findByMemberIdWithLock(10L)).willReturn(Optional.of(member));
            given(jpaRepository.findByMemberIdWithLock(90L)).willReturn(Optional.empty());
            given(jpaRepository.findByIdIn(List.of(1L))).willReturn(List.of(member));

            assertThat(adapter.getById(1L)).isSameAs(member);
            assertThat(adapter.findById(1L)).contains(member);
            assertThat(adapter.getByIdWithLock(1L)).isSameAs(member);
            assertThat(adapter.getByMemberId(10L)).isSameAs(member);
            assertThat(adapter.findByMemberId(10L)).contains(member);
            assertThat(adapter.getByMemberIdWithLock(10L)).isSameAs(member);
            assertThatThrownBy(() -> adapter.getById(9L)).isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> adapter.getByIdWithLock(9L)).isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> adapter.getByMemberId(90L)).isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> adapter.getByMemberIdWithLock(90L)).isInstanceOf(OrganizationDomainException.class);
            assertThat(adapter.listByIds(null)).isEmpty();
            assertThat(adapter.listByIds(List.of())).isEmpty();
            assertThat(adapter.listByIds(List.of(1L))).containsExactly(member);
            adapter.searchIds(null, PageRequest.of(0, 10));
            adapter.existsByMemberId(10L);
            adapter.delete(member);
            then(jpaRepository).should().delete(member);
        }
    }

    @Nested
    @DisplayName("프로덕트 챕터와 소속")
    class ChapterAdapters {
        @Mock
        UmcProductChapterJpaRepository chapterRepository;
        @Mock
        UmcProductChapterMembershipJpaRepository membershipRepository;

        @Test
        @DisplayName("챕터의 단건·잠금·목록·존재·삭제와 not-found를 처리한다")
        void 챕터_adapter_계약을_보장한다() {
            var chapter = 프로덕트_챕터(1L);
            var adapter = new UmcProductChapterPersistenceAdapter(chapterRepository);
            given(chapterRepository.findById(1L)).willReturn(Optional.of(chapter));
            given(chapterRepository.findById(9L)).willReturn(Optional.empty());
            given(chapterRepository.findByIdWithLock(1L)).willReturn(Optional.of(chapter));
            given(chapterRepository.findByIdWithLock(9L)).willReturn(Optional.empty());
            given(chapterRepository.findByIdIn(List.of(1L))).willReturn(List.of(chapter));

            assertThat(adapter.getById(1L)).isSameAs(chapter);
            assertThat(adapter.getByIdWithLock(1L)).isSameAs(chapter);
            assertThatThrownBy(() -> adapter.getById(9L)).isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> adapter.getByIdWithLock(9L)).isInstanceOf(OrganizationDomainException.class);
            adapter.listAll(true);
            assertThat(adapter.listByIds(null)).isEmpty();
            assertThat(adapter.listByIds(List.of(1L))).containsExactly(chapter);
            adapter.existsById(1L);
            adapter.existsByCode("SERVER", null);
            adapter.delete(chapter);
        }

        @Test
        @DisplayName("챕터 소속의 단건·일괄·중복·삭제와 not-found를 처리한다")
        void 챕터_소속_adapter_계약을_보장한다() {
            var member = 프로덕트_멤버(1L, 10L, null);
            var period = 활동_기간(2L, member, START, END);
            var membership = 챕터_소속(3L, period, 프로덕트_챕터(4L), START, END);
            var adapter = new UmcProductChapterMembershipPersistenceAdapter(membershipRepository);
            given(membershipRepository.findById(3L)).willReturn(Optional.of(membership));
            given(membershipRepository.findById(9L)).willReturn(Optional.empty());
            given(membershipRepository.findAllByUmcProductMemberIds(List.of(1L))).willReturn(List.of(membership));

            assertThat(adapter.getById(3L)).isSameAs(membership);
            assertThatThrownBy(() -> adapter.getById(9L)).isInstanceOf(OrganizationDomainException.class);
            adapter.listByUmcProductMemberId(1L);
            assertThat(adapter.listByUmcProductMemberIds(null)).isEmpty();
            assertThat(adapter.listByUmcProductMemberIds(List.of(1L))).containsExactly(membership);
            adapter.existsByChapterId(4L);
            adapter.existsByMemberActivityPeriodId(2L);
            adapter.existsOverlappingChapterMembership(1L, 4L, START, END, null);
            adapter.delete(membership);
            adapter.deleteAllByUmcProductMemberId(1L);
        }
    }

    @Nested
    @DisplayName("활동 기간과 리더십")
    class ActivityAndLeadershipAdapters {
        @Mock
        UmcProductMemberActivityPeriodJpaRepository periodRepository;
        @Mock
        UmcProductLeadershipJpaRepository leadershipRepository;

        @Test
        @DisplayName("활동 기간의 단건·일괄·포함·중복·삭제와 not-found를 처리한다")
        void 활동_기간_adapter_계약을_보장한다() {
            var member = 프로덕트_멤버(1L, 10L, null);
            var period = 활동_기간(2L, member, START, END);
            var adapter = new UmcProductMemberActivityPeriodPersistenceAdapter(periodRepository);
            given(periodRepository.findById(2L)).willReturn(Optional.of(period));
            given(periodRepository.findById(9L)).willReturn(Optional.empty());
            given(periodRepository.findAllByUmcProductMemberIds(List.of(1L))).willReturn(List.of(period));

            assertThat(adapter.getById(2L)).isSameAs(period);
            assertThatThrownBy(() -> adapter.getById(9L)).isInstanceOf(OrganizationDomainException.class);
            adapter.listByUmcProductMemberId(1L);
            assertThat(adapter.listByUmcProductMemberIds(null)).isEmpty();
            assertThat(adapter.listByUmcProductMemberIds(List.of(1L))).containsExactly(period);
            adapter.findContaining(1L, START, END);
            adapter.existsOverlappingOrAdjacent(1L, START, END, null);
            adapter.delete(period);
            adapter.deleteAllByUmcProductMemberId(1L);
        }

        @Test
        @DisplayName("리더십의 단건·일괄·역할·중복·삭제와 입력 경계를 처리한다")
        void 리더십_adapter_계약을_보장한다() {
            var member = 프로덕트_멤버(1L, 10L, null);
            var period = 활동_기간(2L, member, START, END);
            var leadership = 리더십(3L, period, START, END);
            var adapter = new UmcProductLeadershipPersistenceAdapter(leadershipRepository);
            given(leadershipRepository.findById(3L)).willReturn(Optional.of(leadership));
            given(leadershipRepository.findById(9L)).willReturn(Optional.empty());
            given(leadershipRepository.findAllByUmcProductMemberIds(List.of(1L))).willReturn(List.of(leadership));
            given(leadershipRepository.existsByMemberIdAndRolesOnDate(
                10L, Set.of(UmcProductLeadershipRole.UMC_PRODUCT_LEAD), START
            )).willReturn(true);

            assertThat(adapter.getById(3L)).isSameAs(leadership);
            assertThatThrownBy(() -> adapter.getById(9L)).isInstanceOf(OrganizationDomainException.class);
            adapter.listByUmcProductMemberId(1L);
            assertThat(adapter.listByUmcProductMemberIds(null)).isEmpty();
            assertThat(adapter.listByUmcProductMemberIds(List.of(1L))).containsExactly(leadership);
            adapter.existsByMemberActivityPeriodId(2L);
            assertThat(adapter.existsByMemberIdAndRolesOnDate(null, null, null)).isFalse();
            assertThat(adapter.existsByMemberIdAndRolesOnDate(
                10L, Set.of(UmcProductLeadershipRole.UMC_PRODUCT_LEAD), START
            )).isTrue();
            adapter.existsOverlappingRole(UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START, END, null);
            adapter.existsOverlappingMember(1L, START, END, null);
            adapter.delete(leadership);
            adapter.deleteAllByUmcProductMemberId(1L);
        }
    }

    @Nested
    @DisplayName("스쿼드와 참여")
    class SquadAdapters {
        @Mock
        UmcProductSquadJpaRepository squadRepository;
        @Mock
        UmcProductSquadParticipantJpaRepository participantRepository;

        @Test
        @DisplayName("스쿼드의 단건·잠금·목록·존재·삭제와 not-found를 처리한다")
        void 스쿼드_adapter_계약을_보장한다() {
            var squad = 스쿼드(1L, START, END);
            var adapter = new UmcProductSquadPersistenceAdapter(squadRepository);
            given(squadRepository.findById(1L)).willReturn(Optional.of(squad));
            given(squadRepository.findById(9L)).willReturn(Optional.empty());
            given(squadRepository.findByIdWithLock(1L)).willReturn(Optional.of(squad));
            given(squadRepository.findByIdWithLock(9L)).willReturn(Optional.empty());
            given(squadRepository.findByIdIn(List.of(1L))).willReturn(List.of(squad));

            assertThat(adapter.getById(1L)).isSameAs(squad);
            assertThat(adapter.getByIdWithLock(1L)).isSameAs(squad);
            assertThatThrownBy(() -> adapter.getById(9L)).isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> adapter.getByIdWithLock(9L)).isInstanceOf(OrganizationDomainException.class);
            adapter.listAll(true, START);
            assertThat(adapter.listByIds(null)).isEmpty();
            assertThat(adapter.listByIds(List.of(1L))).containsExactly(squad);
            adapter.existsByCode("PLATFORM", null);
            adapter.delete(squad);
        }

        @Test
        @DisplayName("스쿼드 참여의 단건·일괄·중복·삭제와 not-found를 처리한다")
        void 스쿼드_참여_adapter_계약을_보장한다() {
            var member = 프로덕트_멤버(1L, 10L, null);
            var period = 활동_기간(2L, member, START, END);
            var participant = 스쿼드_참여(3L, 스쿼드(4L, START, END), period, START, END);
            var adapter = new UmcProductSquadParticipantPersistenceAdapter(participantRepository);
            given(participantRepository.findById(3L)).willReturn(Optional.of(participant));
            given(participantRepository.findById(9L)).willReturn(Optional.empty());
            given(participantRepository.findAllByUmcProductMemberIds(List.of(1L))).willReturn(List.of(participant));

            assertThat(adapter.getById(3L)).isSameAs(participant);
            assertThatThrownBy(() -> adapter.getById(9L)).isInstanceOf(OrganizationDomainException.class);
            adapter.listBySquadId(4L);
            adapter.listByUmcProductMemberId(1L);
            assertThat(adapter.listByUmcProductMemberIds(null)).isEmpty();
            assertThat(adapter.listByUmcProductMemberIds(List.of(1L))).containsExactly(participant);
            adapter.existsBySquadId(4L);
            adapter.existsByMemberActivityPeriodId(2L);
            adapter.existsOverlappingMemberInSquad(4L, 1L, START, END, null);
            adapter.existsOverlappingSquadLead(4L, START, END, null);
            adapter.delete(participant);
            adapter.deleteAllBySquadId(4L);
            adapter.deleteAllByUmcProductMemberId(1L);
        }
    }

    @Nested
    @DisplayName("제약조건 변환")
    class ConstraintTranslator {
        @Mock
        ConstraintViolationException constraintViolationException;

        @Test
        @DisplayName("제약조건 이름·메시지·미등록 원인을 각각 변환한다")
        void 제약조건을_도메인_예외로_변환한다() {
            Map<String, OrganizationErrorCode> mapping = Map.of(
                "known_constraint", OrganizationErrorCode.UMC_PRODUCT_MEMBER_ALREADY_EXISTS
            );
            given(constraintViolationException.getConstraintName()).willReturn("known_constraint");
            var byName = new DataIntegrityViolationException("outer", constraintViolationException);
            var byMessage = new DataIntegrityViolationException("known_constraint violated");
            var unknown = new DataIntegrityViolationException("unknown");
            var nullMessage = new DataIntegrityViolationException(null);

            assertThat(UmcProductConstraintViolationTranslator.translate(byName, mapping))
                .isInstanceOf(OrganizationDomainException.class);
            assertThat(UmcProductConstraintViolationTranslator.translate(byMessage, mapping))
                .isInstanceOf(OrganizationDomainException.class);
            assertThat(UmcProductConstraintViolationTranslator.translate(unknown, mapping)).isSameAs(unknown);
            assertThat(UmcProductConstraintViolationTranslator.translate(nullMessage, mapping)).isSameAs(nullMessage);
        }
    }
}
