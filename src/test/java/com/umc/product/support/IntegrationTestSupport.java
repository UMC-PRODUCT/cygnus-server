package com.umc.product.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.support.isolation.DatabaseIsolation;

import jakarta.persistence.EntityManager;

/**
 * 통합 테스트(Integration Test) 공통 베이스 클래스.
 *
 * <h2>제공 구성</h2>
 * <ul>
 *   <li>{@code @SpringBootTest} - 전체 ApplicationContext 부트업 (webEnvironment=MOCK)</li>
 *   <li>{@code @ActiveProfiles("test")} - 테스트 프로필 명시 활성화</li>
 *   <li>{@code @Import(TestContainersConfig.class)} - PostgreSQL/PostGIS Testcontainer 기동 및 PostGIS 확장 설치</li>
 *   <li>{@code @DatabaseIsolation} - 각 테스트 종료 후 모든 테이블 TRUNCATE 로 격리 (FK CASCADE, IDENTITY RESTART)</li>
 *   <li>{@code @AutoConfigureMockMvc} - 웹 통합 테스트를 위한 {@link MockMvc} 자동 구성</li>
 *   <li>외부 시스템 의존(메일, JWT, FCM, S3 Storage)은 {@link MockitoBean} 으로 대체</li>
 * </ul>
 *
 * <h2>사용 정책</h2>
 * <ul>
 *   <li><b>본 클래스에는 비즈니스/시나리오 헬퍼를 추가하지 않는다.</b>
 *       데이터 셋업은 {@code com.umc.product.support.fixture} 패키지의 Fixture 컴포넌트를
 *       각 테스트에서 {@link Autowired} 로 주입받아 사용한다.</li>
 *   <li><b>본 클래스에 {@code @Transactional} 을 부여하지 않는다.</b>
 *       각 테스트는 운영과 동일한 트랜잭션 경계로 동작하며, 종료 후 {@code @DatabaseIsolation} 이
 *       물리 TRUNCATE 로 격리를 보장한다. 테스트에 트랜잭션을 거는 순간 지연 로딩 / 1차 캐시 /
 *       Flush 시점 등이 운영과 어긋나 거짓 통과를 만들 수 있다.</li>
 *   <li><b>비결정적 시간/식별자가 검증 대상이라면</b>, 시간은 운영 코드에서 {@code Clock} 빈을 통해 주입받도록
 *       설계하고 테스트에서 고정값으로 대체한다. 자동 증가 ID 는 직접 비교하지 말고
 *       {@code isNotNull()} 또는 도메인 의미 단위로 검증한다.</li>
 *   <li><b>Mockito 빈을 추가로 등록하지 말 것.</b> 외부 시스템(메일/FCM/Storage 등)이
 *       아닌 도메인 빈을 {@code @MockitoBean} 으로 덮어쓰는 것은 통합 테스트의 의미를 훼손한다.
 *       도메인 단위 모킹이 필요하다면 단위 테스트({@code @ExtendWith(MockitoExtension.class)})로 분리한다.</li>
 * </ul>
 *
 * <h2>UseCase 통합 테스트 예시</h2>
 * <pre>{@code
 * class CreateGisuIntegrationTest extends IntegrationTestSupport {
 *
 *     @Autowired
 *     private ManageGisuUseCase manageGisuUseCase;
 *     @Autowired
 *     private GisuFixture gisuFixture;
 *
 *     @Test
 *     void 신규_기수를_생성한다() {
 *         CreateGisuCommand command = new CreateGisuCommand(10L, ...);
 *         Long gisuId = manageGisuUseCase.create(command);
 *         assertThat(gisuId).isNotNull();
 *     }
 * }
 * }</pre>
 *
 * <h2>웹(API) 통합 테스트 예시</h2>
 * <pre>{@code
 * class TermApiIntegrationTest extends IntegrationTestSupport {
 *
 *     @Autowired
 *     private TermFixture termFixture;
 *
 *     @Test
 *     void 활성_약관_조회() throws Exception {
 *         termFixture.필수_약관(TermType.SERVICE);
 *         mockMvc.perform(get("/api/v1/terms/SERVICE"))
 *                .andExpect(status().isOk());
 *     }
 * }
 * }</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Testcontainers
@DatabaseIsolation
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    /**
     * API 통합 테스트용 MockMvc. {@code @AutoConfigureMockMvc} 에 의해 자동 주입된다.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * 요청/응답 직렬화 검증, 기대값 비교 등에 활용한다.
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Port 가 노출하지 않는 영속 상태를 직접 확인할 때 사용한다. 가능하면 Port/Query UseCase 사용을 우선한다.
     */
    @Autowired
    protected EntityManager entityManager;

    // =========================================================================
    // 외부 시스템 연동: 통합 테스트에서는 항상 Mock 으로 대체한다.
    // 도메인 / 애플리케이션 빈을 임의로 모킹하지 말 것.
    // =========================================================================

    @MockitoBean
    protected JavaMailSender mailSender;

    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    protected FirebaseMessaging firebaseMessaging;

    @MockitoBean
    protected StoragePort storagePort;

    @BeforeEach
    void setUpDefaultJwtTermClaims() {
        given(jwtTokenProvider.hasRequiredTermsAgreed(anyString())).willReturn(true);
        given(jwtTokenProvider.getAgreedRequiredTermIdsFromAccessToken(anyString())).willReturn(List.of());
    }
}
