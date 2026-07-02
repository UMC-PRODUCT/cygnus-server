package com.umc.product.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

class GlobalResponseWrapperTest {

    private final GlobalResponseWrapper sut = new GlobalResponseWrapper();

    @Test
    @DisplayName("ResponseEntity 반환은 전역 응답 래핑 대상에서 제외한다")
    void ResponseEntity_반환은_전역_응답_래핑_대상에서_제외한다() throws Exception {
        // given
        MethodParameter returnType = returnType("responseEntity");

        // when
        boolean result = sut.supports(returnType, converter(ByteArrayHttpMessageConverter.class));

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("일반 DTO 반환은 전역 응답 래핑 대상이다")
    void 일반_DTO_반환은_전역_응답_래핑_대상이다() throws Exception {
        // given
        MethodParameter returnType = returnType("dto");

        // when
        boolean result = sut.supports(returnType, converter(MappingJackson2HttpMessageConverter.class));

        // then
        assertThat(result).isTrue();
    }

    private MethodParameter returnType(String methodName) throws NoSuchMethodException {
        Method method = SampleController.class.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends HttpMessageConverter<?>> converter(
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return (Class<? extends HttpMessageConverter<?>>) converterType;
    }

    private static class SampleController {

        ResponseEntity<byte[]> responseEntity() {
            return ResponseEntity.ok(new byte[0]);
        }

        SampleResponse dto() {
            return new SampleResponse("ok");
        }
    }

    private record SampleResponse(String value) {
    }
}
