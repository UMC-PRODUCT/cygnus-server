package com.umc.product.challenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.umc.product.authorization.adapter.out.persistence.ChallengerRoleJpaRepository;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.challenger.adapter.out.persistence.ChallengerJpaRepository;
import com.umc.product.challenger.adapter.out.persistence.ChallengerRecordJpaRepository;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.out.command.SaveGisuPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.SchoolFixture;

@DisplayName("코드 한 번으로 기수 수강과 운영진 역할 등록")
class ChallengerRecordRegistrationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ManageChallengerRecordUseCase records;
    @Autowired
    private SaveGisuPort saveGisuPort;
    @Autowired
    private ChapterFixture chapterFixture;
    @Autowired
    private SchoolFixture schoolFixture;
    @Autowired
    private SaveMemberPort saveMemberPort;
    @Autowired
    private SaveChallengerPort saveChallengerPort;
    @Autowired
    private ChallengerJpaRepository challengers;
    @Autowired
    private ChallengerRecordJpaRepository recordRepository;
    @Autowired
    private ChallengerRoleJpaRepository roles;
    @Autowired
    private ManageChallengerRoleUseCase manageRoles;
    @Autowired
    private CheckPermissionUseCase permissions;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private SendWebhookAlarmUseCase notifications;

    private Gisu gisu;
    private Chapter chapter;
    private School school;
    private Member member;

    @BeforeEach
    void 준비() {
        gisu = saveGisuPort.save(Gisu.create(9310L,
            Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2027-02-28T00:00:00Z"),
            false, GisuLearningType.TRACK));
        chapter = chapterFixture.지부(gisu, "통합등록지부");
        school = schoolFixture.지부에_소속된_학교("통합등록학교", chapter);
        member = saveMemberPort.save(Member.create("등록회원", "등록", "register@test.com", school.getId(), null));
    }

    @Test
    @DisplayName("신규 회원이 복수 트랙과 학교 회장 역할을 한 번에 등록한다")
    void 신규_수강과_역할_등록() {
        ChallengerRecord record = issue(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.DESIGN),
            ChallengerRoleType.SCHOOL_PRESIDENT);

        consume(record);

        Challenger challenger = membership();
        assertThat(challenger.getPart()).isNull();
        assertThat(challenger.getTracks()).containsExactly(
            ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.DESIGN);
        assertThat(roles.findByChallengerId(challenger.getId())).singleElement().satisfies(role -> {
            assertThat(role.getChallengerRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
            assertThat(role.getOrganizationId()).isEqualTo(school.getId());
            assertThat(role.getGisuId()).isEqualTo(gisu.getId());
        });
        assertUsed(record);
    }

    @Test
    @DisplayName("수강 없는 학교 운영진이 빈 트랙 소속과 역할을 한 번에 등록한다")
    void 비수강_학교_운영진_등록() {
        ChallengerRecord record = issue(List.of(), ChallengerRoleType.SCHOOL_VICE_PRESIDENT);

        consume(record);

        assertThat(membership().getTracks()).isEmpty();
        assertThat(membership().getPart()).isNull();
        assertThat(roles.findByChallengerId(membership().getId())).hasSize(1);
        assertUsed(record);
    }

    @Test
    @DisplayName("지부 없는 중앙 운영진이 가입하고 중앙 권한을 조회한다")
    void 지부_없는_중앙_운영진_등록과_권한조회() {
        school = schoolFixture.학교("중앙만참여학교");
        member = saveMemberPort.save(Member.create("중앙회원", "중앙", "central@test.com", school.getId(), null));
        Long id = records.create(command(List.of(), ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER)
            .chapterId(null).build());

        consume(recordRepository.findById(id).orElseThrow());

        var snapshot = permissions.loadSubject(member.getId()).toAuthoritySnapshot();
        assertThat(snapshot.isCentralMemberInGisu(gisu.getId())).isTrue();
        assertThat(snapshot.isSchoolCoreInGisu(gisu.getId(), school.getId())).isFalse();
        assertThat(snapshot.gisuChallengerInfos()).singleElement()
            .satisfies(info -> assertThat(info.chapterId()).isNull());
        assertThat(membership().getTracks()).isEmpty();
    }

    @Test
    @DisplayName("기존 웹 수강생은 웹과 회장 코드로 역할만 추가하고 기존 소속과 다른 역할을 보존한다")
    void 기존_소속과_수강과_역할_보존() {
        Challenger existing = saveChallengerPort.save(Challenger.builder().memberId(member.getId())
            .gisuId(gisu.getId()).tracks(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER)).build());
        Long oldRole = manageRoles.createChallengerRole(CreateChallengerRoleCommand.builder()
            .challengerId(existing.getId()).gisuId(gisu.getId()).organizationId(chapter.getId())
            .roleType(ChallengerRoleType.CHAPTER_PRESIDENT).build());
        ChallengerRecord record = issue(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
            ChallengerRoleType.SCHOOL_PRESIDENT);

        consume(record);

        assertThat(membership().getId()).isEqualTo(existing.getId());
        assertThat(membership().getTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(roles.findByChallengerId(existing.getId())).hasSize(2)
            .anySatisfy(role -> assertThat(role.getId()).isEqualTo(oldRole));
        assertUsed(record);
    }

    @Test
    @DisplayName("겸직자는 역할별 코드를 두 번 등록해도 한 소속에 수강과 두 역할을 보존한다")
    void 역할별_코드로_겸직_등록() {
        ChallengerRecord central = issue(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER);
        ChallengerRecord president = issue(List.of(), ChallengerRoleType.SCHOOL_PRESIDENT);

        consume(central);
        Long challengerId = membership().getId();
        consume(president);

        assertThat(challengers.findByMemberId(member.getId())).hasSize(1);
        assertThat(membership().getId()).isEqualTo(challengerId);
        assertThat(membership().getTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(roles.findByChallengerId(challengerId)).extracting(role -> role.getChallengerRoleType())
            .containsExactlyInAnyOrder(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
                ChallengerRoleType.SCHOOL_PRESIDENT);
        assertUsed(central);
        assertUsed(president);
    }

    @Test
    @DisplayName("같은 역할은 중복 생성하지 않고 코드에 추가된 트랙만 등록한다")
    void 기존_역할을_보존하며_트랙_추가() {
        consume(issue(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER), ChallengerRoleType.SCHOOL_PRESIDENT));
        Long id = membership().getId();
        ChallengerRecord record = issue(List.of(ChallengerTrack.DESIGN), ChallengerRoleType.SCHOOL_PRESIDENT);

        consume(record);

        assertThat(membership().getId()).isEqualTo(id);
        assertThat(membership().getTracks()).containsExactly(
            ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.DESIGN);
        assertThat(roles.findByChallengerId(id)).hasSize(1);
        assertUsed(record);
    }

    @Test
    @DisplayName("코드의 수강과 역할을 이미 모두 갖고 있으면 코드를 사용하지 않는다")
    void 완전_중복_코드_거절() {
        consume(issue(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER), ChallengerRoleType.SCHOOL_PRESIDENT));
        ChallengerRecord duplicate = issue(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
            ChallengerRoleType.SCHOOL_PRESIDENT);

        assertThatThrownBy(() -> consume(duplicate)).isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode").isEqualTo(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS);

        assertThat(recordRepository.findById(duplicate.getId()).orElseThrow().isUsed()).isFalse();
        assertThat(roles.findByChallengerId(membership().getId())).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("운영진 코드도 이름이나 학교가 다르면 아무 소속과 역할을 만들지 않는다")
    void 운영진도_회원_일치_검증(boolean wrongName) {
        ChallengerRecord record = issue(List.of(), ChallengerRoleType.SCHOOL_PRESIDENT);
        Long otherSchool = wrongName ? school.getId() : schoolFixture.학교("다른학교").getId();
        Member other = saveMemberPort.save(Member.create(wrongName ? "다른이름" : member.getName(), "불일치",
            "mismatch@test.com", otherSchool, null));

        assertThatThrownBy(() -> consume(record, other.getId())).isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode").isEqualTo(wrongName ? ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD
                : ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD);

        assertThat(challengers.findByMemberId(other.getId())).isEmpty();
        assertThat(recordRepository.findById(record.getId()).orElseThrow().isUsed()).isFalse();
        assertThat(roles.count()).isZero();
    }

    @Test
    @DisplayName("비활성 트랙 소속은 코드로 자동 복구하지 않는다")
    void 비활성_소속_거절() {
        Challenger inactive = Challenger.createWithoutEnrollment(member.getId(), gisu.getId());
        inactive.changeStatus(ChallengerStatus.WITHDRAWN, member.getId(), "중도탈퇴");
        saveChallengerPort.save(inactive);
        ChallengerRecord record = issue(List.of(ChallengerTrack.DESIGN), ChallengerRoleType.SCHOOL_PRESIDENT);

        assertThatThrownBy(() -> consume(record)).isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode").isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE);

        assertThat(membership().getStatus()).isEqualTo(ChallengerStatus.WITHDRAWN);
        assertThat(roles.count()).isZero();
        assertThat(recordRepository.findById(record.getId()).orElseThrow().isUsed()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("등록 중 실패하면 신규 소속 또는 기존 수강 변경과 역할 및 코드 사용을 모두 롤백한다")
    void 등록_전체_롤백(boolean existingMembership) {
        if (existingMembership) {
            consume(issue(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER), null));
        }
        ChallengerRecord record = issue(List.of(ChallengerTrack.DESIGN), ChallengerRoleType.SCHOOL_PRESIDENT);
        doThrow(new IllegalStateException("등록 후처리 실패")).when(notifications).sendBuffered(any());

        assertThatThrownBy(() -> consume(record)).isInstanceOf(IllegalStateException.class);

        if (existingMembership) {
            assertThat(membership().getTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        } else {
            assertThat(challengers.findByMemberId(member.getId())).isEmpty();
        }
        assertThat(roles.count()).isZero();
        assertThat(recordRepository.findById(record.getId()).orElseThrow().isUsed()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("같은 회원의 서로 다른 코드는 직렬화되어 하나의 소속에 두 수강을 모두 남긴다")
    void 서로_다른_코드의_동시_등록(boolean existingMembership) throws Exception {
        if (existingMembership) {
            saveChallengerPort.save(Challenger.createWithoutEnrollment(member.getId(), gisu.getId()));
        }
        ChallengerRecord firstRecord = issue(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
            ChallengerRoleType.SCHOOL_PRESIDENT);
        ChallengerRecord secondRecord = issue(List.of(ChallengerTrack.DESIGN), ChallengerRoleType.SCHOOL_PRESIDENT);
        CountDownLatch firstConsumed = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);

        try (PostgreSqlTransactionRace race = new PostgreSqlTransactionRace(transactionManager, jdbcTemplate)) {
            var first = race.submit(() -> {
                consume(firstRecord);
                firstConsumed.countDown();
                race.await(allowCommit, "첫 등록 커밋 대기");
                return null;
            });
            race.await(firstConsumed, "첫 등록 완료");
            var second = race.submit(() -> {
                consume(secondRecord);
                return null;
            });
            try {
                race.awaitPostgreSqlLockWait(second);
                assertThat(second.future().isDone()).isFalse();
            } finally {
                allowCommit.countDown();
            }
            first.future().get(10, TimeUnit.SECONDS);
            second.future().get(10, TimeUnit.SECONDS);
        }

        assertThat(challengers.findByMemberId(member.getId())).hasSize(1);
        assertThat(membership().getTracks()).containsExactly(
            ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.DESIGN);
        assertThat(roles.findByChallengerId(membership().getId())).hasSize(1);
        assertUsed(firstRecord);
        assertUsed(secondRecord);
    }

    @Test
    @DisplayName("기존 ADMIN 입력은 트랙 기수의 빈 수강 소속으로 등록한다")
    void ADMIN_입력_호환() {
        Long id = records.create(command(List.of(), null).part(ChallengerPart.ADMIN).build());

        consume(recordRepository.findById(id).orElseThrow());

        assertThat(membership().getTracks()).isEmpty();
        assertThat(membership().getPart()).isNull();
        assertThat(roles.count()).isZero();
    }

    private CreateChallengerRecordCommand.CreateChallengerRecordCommandBuilder command(
        List<ChallengerTrack> tracks, ChallengerRoleType role
    ) {
        return CreateChallengerRecordCommand.builder().creatorMemberId(member.getId())
            .gisuId(gisu.getId()).chapterId(chapter.getId()).schoolId(school.getId())
            .memberName(member.getName()).tracks(tracks).challengerRoleType(role);
    }

    private ChallengerRecord issue(List<ChallengerTrack> tracks, ChallengerRoleType role) {
        return recordRepository.findById(records.create(command(tracks, role).build())).orElseThrow();
    }

    private void consume(ChallengerRecord record) {
        consume(record, member.getId());
    }

    private void consume(ChallengerRecord record, Long memberId) {
        records.consumeCode(ConsumeChallengerRecordCommand.builder()
            .targetMemberId(memberId).code(record.getCode()).build());
    }

    private Challenger membership() {
        return challengers.findByMemberIdAndGisuId(member.getId(), gisu.getId()).orElseThrow();
    }

    private void assertUsed(ChallengerRecord record) {
        ChallengerRecord saved = recordRepository.findById(record.getId()).orElseThrow();
        assertThat(saved.isUsed()).isTrue();
        assertThat(saved.getUsedMemberId()).isEqualTo(member.getId());
    }
}
