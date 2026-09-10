package com.umc.product.challenger.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

import com.umc.product.authorization.adapter.out.persistence.ChallengerRoleJpaRepository;
import com.umc.product.challenger.adapter.in.web.assembler.ChallengerRecordResponseAssembler;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse;
import com.umc.product.challenger.adapter.out.persistence.ChallengerJpaRepository;
import com.umc.product.challenger.adapter.out.persistence.ChallengerRecordJpaRepository;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.application.port.out.SaveChallengerRecordPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.out.command.SaveGisuPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace.TransactionCall;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;

@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChallengerRecordController 통합 테스트")
class ChallengerRecordControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private LoadGisuPort loadGisuPort;

    @Autowired
    private ChapterFixture chapterFixture;

    @Autowired
    private SchoolFixture schoolFixture;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private SaveChallengerPort saveChallengerPort;

    @Autowired
    private SaveChallengerRecordPort saveChallengerRecordPort;

    @Autowired
    private ChallengerJpaRepository challengerJpaRepository;

    @Autowired
    private ChallengerRecordJpaRepository challengerRecordJpaRepository;

    @Autowired
    private ChallengerRoleJpaRepository challengerRoleJpaRepository;

    @Autowired
    private ManageChallengerRecordUseCase manageChallengerRecordUseCase;

    @Autowired
    private ChallengerRecordResponseAssembler recordResponseAssembler;

    @Autowired
    private SaveGisuPort saveGisuPort;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("일반 챌린저 기록 코드를 소비하면 챌린저가 생성되고 코드가 사용 처리된다")
    void 일반_챌린저_기록_코드를_소비하면_챌린저가_생성되고_코드가_사용_처리된다() throws Exception {
        // given
        RecordContext context = recordContext(9201L, "일반코드");
        Member member = member("홍길동", "길동", "regular-code@test.com", context.school().getId());
        Member creator = member("관리자", "관리", "regular-code-admin@test.com", context.school().getId());
        ChallengerRecord record = saveChallengerRecordPort.save(ChallengerRecord.create(
            creator.getId(),
            context.gisu().getId(),
            context.chapter().getId(),
            context.school().getId(),
            ChallengerPart.SPRINGBOOT,
            member.getName()
        ));
        authenticate(member.getId());

        // when & then
        mockMvc.perform(post("/api/v1/challenger-record/member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(codeRequest(record.getCode())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        Challenger savedChallenger = challengerJpaRepository
            .findByMemberIdAndGisuId(member.getId(), context.gisu().getId())
            .orElseThrow();
        assertThat(savedChallenger.getPart()).isEqualTo(ChallengerPart.SPRINGBOOT);

        ChallengerRecord usedRecord = challengerRecordJpaRepository.findByCode(record.getCode()).orElseThrow();
        assertThat(usedRecord.isUsed()).isTrue();
        assertThat(usedRecord.getUsedMemberId()).isEqualTo(member.getId());
        assertThat(usedRecord.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("운영진 기록 코드를 소비하면 기존 챌린저에 역할이 부여되고 새 챌린저는 만들지 않는다")
    void 운영진_기록_코드를_소비하면_기존_챌린저에_역할이_부여되고_새_챌린저는_만들지_않는다() throws Exception {
        // given
        RecordContext context = recordContext(9202L, "운영진코드");
        Member member = member("김운영", "운영", "admin-code@test.com", context.school().getId());
        Member creator = member("관리자", "관리", "admin-code-admin@test.com", context.school().getId());
        Challenger challenger = saveChallengerPort.save(Challenger.builder()
            .memberId(member.getId())
            .part(ChallengerPart.WEB)
            .gisuId(context.gisu().getId())
            .build());
        ChallengerRecord record = saveChallengerRecordPort.save(ChallengerRecord.createAdmin(
            creator.getId(),
            context.gisu().getId(),
            context.chapter().getId(),
            context.school().getId(),
            ChallengerPart.WEB,
            member.getName(),
            ChallengerRoleType.SCHOOL_PRESIDENT,
            context.school().getId()
        ));
        authenticate(member.getId());

        // when & then
        mockMvc.perform(post("/api/v1/challenger-record/member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(codeRequest(record.getCode())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertThat(challengerJpaRepository.findByMemberId(member.getId()))
            .extracting(Challenger::getId)
            .containsExactly(challenger.getId());

        assertThat(challengerRoleJpaRepository.findByChallengerId(challenger.getId()))
            .hasSize(1)
            .first()
            .satisfies(role -> {
                assertThat(role.getChallengerRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
                assertThat(role.getOrganizationId()).isEqualTo(context.school().getId());
                assertThat(role.getGisuId()).isEqualTo(context.gisu().getId());
            });

        ChallengerRecord usedRecord = challengerRecordJpaRepository.findByCode(record.getCode()).orElseThrow();
        assertThat(usedRecord.isUsed()).isTrue();
        assertThat(usedRecord.getUsedMemberId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("이미 사용된 챌린저 기록 코드는 재사용할 수 없다")
    void 이미_사용된_챌린저_기록_코드는_재사용할_수_없다() throws Exception {
        // given
        RecordContext context = recordContext(9203L, "재사용실패");
        Member member = member("박실패", "실패", "reused-code@test.com", context.school().getId());
        Member creator = member("관리자", "관리", "reused-code-admin@test.com", context.school().getId());
        ChallengerRecord record = saveChallengerRecordPort.save(ChallengerRecord.create(
            creator.getId(),
            context.gisu().getId(),
            context.chapter().getId(),
            context.school().getId(),
            ChallengerPart.ANDROID,
            member.getName()
        ));
        authenticate(member.getId());

        mockMvc.perform(post("/api/v1/challenger-record/member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(codeRequest(record.getCode())))
            .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post("/api/v1/challenger-record/member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(codeRequest(record.getCode())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("CHALLENGER-0012"));

        assertThat(challengerJpaRepository.findByMemberId(member.getId()))
            .hasSize(1)
            .first()
            .extracting(Challenger::getPart)
            .isEqualTo(ChallengerPart.ANDROID);

        ChallengerRecord usedRecord = challengerRecordJpaRepository.findByCode(record.getCode()).orElseThrow();
        assertThat(usedRecord.isUsed()).isTrue();
        assertThat(usedRecord.getUsedMemberId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("비활성 트랙 기수의 코드를 발급하고 등록하면 기본 트랙 하나가 저장된다")
    void 비활성_트랙_기수의_코드를_발급하고_등록하면_기본_트랙_하나가_저장된다() throws Exception {
        // given
        Gisu gisu = saveGisuPort.save(Gisu.create(
            9301L, Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2027-02-01T00:00:00Z"),
            false, GisuLearningType.TRACK));
        Chapter chapter = chapterFixture.지부(gisu, "트랙코드지부");
        School school = schoolFixture.지부에_소속된_학교("트랙코드학교", chapter);
        Member member = member("트랙회원", "트랙", "track-code@test.com", school.getId());
        Long recordId = manageChallengerRecordUseCase.create(CreateChallengerRecordCommand.builder()
            .creatorMemberId(member.getId()).gisuId(gisu.getId()).chapterId(chapter.getId())
            .schoolId(school.getId()).track(ChallengerTrack.WEB_PRODUCT_ENGINEER)
            .memberName(member.getName()).build());
        ChallengerRecord record = challengerRecordJpaRepository.findById(recordId).orElseThrow();
        ChallengerRecordResponse response = recordResponseAssembler.from(recordId);
        assertThat(response.chapterId()).isEqualTo(chapter.getId());
        assertThat(response.chapterName()).isEqualTo(chapter.getName());
        assertThat(response.track()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        authenticate(member.getId());

        // when
        mockMvc.perform(post("/api/v1/challenger-record/member")
                .contentType(MediaType.APPLICATION_JSON).content(codeRequest(record.getCode())))
            .andExpect(status().isOk());

        // then
        Challenger challenger = challengerJpaRepository.findByMemberIdAndGisuId(member.getId(), gisu.getId())
            .orElseThrow();
        assertThat(challenger.getPart()).isNull();
        assertThat(challenger.getTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(challengerRecordJpaRepository.findById(recordId).orElseThrow().isUsed()).isTrue();
    }

    @Test
    @DisplayName("같은 코드의 동시 등록은 첫 트랜잭션을 기다린 뒤 이미 사용된 코드로 거절한다")
    void 같은_코드의_동시_등록은_첫_트랜잭션을_기다린_뒤_이미_사용된_코드로_거절한다() throws Exception {
        // given
        RecordContext context = recordContext(9302L, "코드동시성");
        Member firstMember = member("동명이인", "첫회원", "code-race-first@test.com", context.school().getId());
        Member secondMember = member("동명이인", "둘회원", "code-race-second@test.com", context.school().getId());
        ChallengerRecord record = saveChallengerRecordPort.save(ChallengerRecord.create(
            firstMember.getId(), context.gisu().getId(), context.chapter().getId(), context.school().getId(),
            ChallengerPart.WEB, firstMember.getName()));
        CountDownLatch firstConsumed = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);

        try (PostgreSqlTransactionRace race = new PostgreSqlTransactionRace(transactionManager, jdbcTemplate)) {
            TransactionCall<Void> first = race.submit(() -> {
                manageChallengerRecordUseCase.consumeCode(ConsumeChallengerRecordCommand.builder()
                    .targetMemberId(firstMember.getId()).code(record.getCode()).build());
                firstConsumed.countDown();
                race.await(allowFirstCommit, "첫 코드 소비 커밋 대기");
                return null;
            });
            race.await(firstConsumed, "첫 코드 소비 완료");

            // when
            TransactionCall<Void> second = race.submit(() -> {
                manageChallengerRecordUseCase.consumeCode(ConsumeChallengerRecordCommand.builder()
                    .targetMemberId(secondMember.getId()).code(record.getCode()).build());
                return null;
            });
            try {
                race.awaitPostgreSqlLockWait(second);
                assertThat(second.future().isDone()).isFalse();
            } finally {
                allowFirstCommit.countDown();
            }
            first.future().get(10, TimeUnit.SECONDS);

            // then
            ExecutionException failure = assertThrows(ExecutionException.class,
                () -> second.future().get(10, TimeUnit.SECONDS));
            assertThat(failure.getCause()).isInstanceOf(ChallengerDomainException.class)
                .extracting(error -> ((ChallengerDomainException) error).getBaseCode())
                .isEqualTo(ChallengerErrorCode.USED_CHALLENGER_RECORD_CODE);
        }
        assertThat(challengerJpaRepository.findByMemberIdAndGisuId(firstMember.getId(), context.gisu().getId()))
            .isPresent();
        assertThat(challengerJpaRepository.findByMemberIdAndGisuId(secondMember.getId(), context.gisu().getId()))
            .isEmpty();
        assertThat(challengerRecordJpaRepository.findById(record.getId()).orElseThrow().getUsedMemberId())
            .isEqualTo(firstMember.getId());
    }

    private RecordContext recordContext(Long generation, String prefix) {
        Gisu gisu = loadGisuPort.findActiveGisu()
            .orElseGet(() -> gisuFixture.활성_기수(generation));
        Chapter chapter = chapterFixture.지부(gisu, prefix + "지부");
        School school = schoolFixture.지부에_소속된_학교(prefix + "학교", chapter);
        return new RecordContext(gisu, chapter, school);
    }

    private Member member(String name, String nickname, String email, Long schoolId) {
        return saveMemberPort.save(Member.create(name, nickname, email, schoolId, null));
    }

    private void authenticate(Long memberId) {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(memberId)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private String codeRequest(String code) {
        return """
            {"code":"%s"}
            """.formatted(code);
    }

    private record RecordContext(Gisu gisu, Chapter chapter, School school) {
    }
}
