/**
 * 통합 테스트 공통 인프라 패키지.
 *
 * <h2>구성</h2>
 * <ul>
 *   <li>{@link com.umc.product.support.IntegrationTestSupport} - 통합 테스트 베이스 클래스</li>
 *   <li>{@link com.umc.product.support.TestContainersConfig} - PostgreSQL/PostGIS Testcontainer 구성</li>
 *   <li>{@link com.umc.product.support.ControllerTestSupport} - MockMvc 슬라이스 테스트 베이스</li>
 *   <li>{@code com.umc.product.support.isolation} - 테스트 간 DB 격리(@DatabaseIsolation TRUNCATE)</li>
 *   <li>{@code com.umc.product.support.fixture} - 도메인별 영속화 Fixture</li>
 * </ul>
 *
 * <h2>통합 테스트 작성 절차</h2>
 * <ol>
 *   <li>테스트 클래스가 {@link com.umc.product.support.IntegrationTestSupport} 를 상속하도록 한다.</li>
 *   <li>필요한 Fixture / UseCase / Port 를 {@code @Autowired} 로 주입한다.</li>
 *   <li>Given: Fixture 로 사전 데이터를 적재한다.</li>
 *   <li>When: 검증 대상 UseCase 또는 MockMvc 호출을 수행한다.</li>
 *   <li>Then: Port 또는 Query UseCase 를 통해 영속 결과를 검증한다.
 *       Port 가 노출하지 않는 상태가 필요한 경우에 한해 {@code EntityManager} 를 사용한다.</li>
 * </ol>
 *
 * <p>예시는 {@link com.umc.product.support.IntegrationTestSupport} 의 Javadoc 또는
 * {@code com.umc.product.integration.term.TermAgreementIntegrationTest} 를 참고하라.</p>
 *
 * <h2>주의 사항</h2>
 * <ul>
 *   <li>본 패키지의 베이스 클래스에 비즈니스 로직 / 도메인 헬퍼를 추가하지 않는다.
 *       데이터 셋업은 Fixture 컴포넌트에 위임한다.</li>
 *   <li>테스트 메서드 / 클래스에 {@code @Transactional} 을 부여하지 않는다.
 *       격리는 {@code @DatabaseIsolation} 의 TRUNCATE 로 보장된다.</li>
 *   <li>외부 시스템(메일/JWT/FCM/S3) 외의 도메인 빈을 {@code @MockitoBean} 으로
 *       덮어쓰지 않는다. 도메인 모킹이 필요한 케이스는 단위 테스트로 분리한다.</li>
 * </ul>
 */
package com.umc.product.support;
