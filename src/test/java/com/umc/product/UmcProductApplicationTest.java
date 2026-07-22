package com.umc.product;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

@DisplayName("UMC PRODUCT 애플리케이션 진입점")
class UmcProductApplicationTest {

    @Test
    @DisplayName("main은 전달받은 인자로 Spring Boot 애플리케이션을 시작한다")
    void main은_Spring_Boot를_시작한다() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            UmcProductApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(UmcProductApplication.class, args));
        }
    }
}
