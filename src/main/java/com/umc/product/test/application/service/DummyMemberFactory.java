package com.umc.product.test.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.datafaker.Faker;

import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.term.application.port.in.query.GetTermUseCase;

import lombok.RequiredArgsConstructor;

/**
 * datafaker 를 사용해 test 도메인 시딩용 더미 이메일 회원 Command 를 생성한다. ADR-017 참조.
 * <p>
 * V2026.02.28.06.00 마이그레이션이 school 1~38 을 시딩하므로 schoolId 무작위 추출은 그 범위에서 고른다.
 * email 은 sequence 번호로 고정해 멱등성과 유일성을 동시에 만족시킨다. 모든 더미 회원은
 * 같은 비밀번호({@link SeedProperties#defaultPassword()})를 사용한다.
 * <p>
 * <b>약관 동의 캐싱</b>: 시딩은 N 명을 한 번에 만들기 때문에 매 멤버마다
 * {@code GetTermUseCase.getRequiredTermIds()} 를 호출하면 N 회 SELECT 가 발생한다. 호출자가
 * {@link #snapshotMandatoryConsents()} 로 1 회만 조회하고 그 결과를
 * {@link #nextEmailCommand(long, List)} 등의 인자로 재사용하면 약관 조회 횟수가 1 회로 줄어든다.
 */
@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DummyMemberFactory {

    private static final long MIN_SCHOOL_ID = 1L;
    private static final long MAX_SCHOOL_ID = 38L;
    private static final int MAX_NICKNAME_LENGTH = 20;

    private final SeedProperties properties;
    private final GetTermUseCase getTermUseCase;

    private final Faker faker = new Faker(Locale.KOREAN);

    /**
     * 현재 활성 필수 약관 전체에 대해 isAgreed=true 인 동의 목록 스냅샷을 만든다.
     * 시딩 호출 1 회당 1 번만 호출해 후속 {@code next*} 메서드에 인자로 넘기면
     * 약관 조회 SELECT 가 N → 1 회로 감소한다.
     */
    public List<TermConsents> snapshotMandatoryConsents() {
        Set<Long> requiredTermIds = getTermUseCase.getRequiredTermIds();
        return requiredTermIds.stream()
            .map(termId -> TermConsents.builder().termId(termId).isAgreed(true).build())
            .toList();
    }

    /**
     * 이메일 회원가입용 Command 를 만든다. email 은 alpha_user_0001@{properties.emailDomain} 형식.
     * schoolId 는 1~38 범위 무작위.
     */
    public EmailRegisterMemberCommand nextEmailCommand(long sequence, List<TermConsents> termConsents) {
        return nextEmailCommandWithSchool(sequence, randomSchoolId(), termConsents);
    }

    /**
     * 지정한 schoolId 로 이메일 회원가입 Command 를 만든다. 챌린저/프로젝트 분포 시딩처럼 학교 정합성이
     * 필요한 케이스에서 사용한다.
     */
    public EmailRegisterMemberCommand nextEmailCommandWithSchool(
        long sequence, Long schoolId, List<TermConsents> termConsents
    ) {
        String email = "alpha_user_%04d@%s".formatted(sequence, properties.emailDomain());
        return EmailRegisterMemberCommand.builder()
            .rawPassword(properties.defaultPassword())
            .name(faker.name().fullName())
            .nickname(safeNickname(faker.name().firstName(), sequence))
            .email(email)
            .schoolId(schoolId)
            .termConsents(termConsents)
            .build();
    }

    private long randomSchoolId() {
        return ThreadLocalRandom.current().nextLong(MIN_SCHOOL_ID, MAX_SCHOOL_ID + 1);
    }

    /**
     * Member.nickname 컬럼 길이 20 을 초과하지 않게 자르고, sequence 를 붙여 유일성을 보강한다.
     */
    String safeNickname(String base, long sequence) {
        String candidate = base + sequence;
        if (candidate.length() > MAX_NICKNAME_LENGTH) {
            return candidate.substring(0, MAX_NICKNAME_LENGTH);
        }
        return candidate;
    }
}
