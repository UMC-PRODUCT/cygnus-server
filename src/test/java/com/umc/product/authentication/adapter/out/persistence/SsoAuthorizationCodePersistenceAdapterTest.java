package com.umc.product.authentication.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.authentication.domain.PkceChallengeMethod;
import com.umc.product.authentication.domain.SsoAuthorizationCode;
import com.umc.product.global.config.JpaConfig;
import com.umc.product.global.config.QueryDslConfig;
import com.umc.product.support.TestContainersConfig;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QueryDslConfig.class, TestContainersConfig.class, SsoAuthorizationCodePersistenceAdapter.class})
@DisplayName("SsoAuthorizationCodePersistenceAdapter")
class SsoAuthorizationCodePersistenceAdapterTest {

    private static final String CODE_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final Long MEMBER_ID = 1L;
    private static final String CLIENT_ID = "backoffice";
    private static final String REDIRECT_URI = "https://backoffice.university.neordinary.com/auth/callback";
    private static final String CODE_CHALLENGE = "challenge";

    @Autowired
    TestEntityManager em;

    @Autowired
    SsoAuthorizationCodePersistenceAdapter adapter;

    @Test
    @DisplayName("SSO authorization code를 저장하고 code hash로 조회한다")
    void sso_authorization_code_저장_후_code_hash_조회() {
        // given
        SsoAuthorizationCode authorizationCode = createCode(CODE_HASH);

        // when
        adapter.save(authorizationCode);
        em.flush();
        em.clear();

        // then
        Optional<SsoAuthorizationCode> found = adapter.findByCodeHashForUpdate(CODE_HASH);
        assertThat(found).isPresent();
        assertThat(found.get().getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(found.get().getClientId()).isEqualTo(CLIENT_ID);
        assertThat(found.get().getRedirectUri()).isEqualTo(REDIRECT_URI);
    }

    @Test
    @DisplayName("없는 code hash로 조회하면 빈 Optional을 반환한다")
    void sso_authorization_code_없으면_empty_반환() {
        // when
        Optional<SsoAuthorizationCode> found = adapter.findByCodeHashForUpdate("missing-hash");

        // then
        assertThat(found).isEmpty();
    }

    private SsoAuthorizationCode createCode(String codeHash) {
        return SsoAuthorizationCode.create(
            codeHash,
            MEMBER_ID,
            CLIENT_ID,
            REDIRECT_URI,
            CODE_CHALLENGE,
            PkceChallengeMethod.S256,
            Instant.parse("2026-06-26T00:03:00Z")
        );
    }
}
