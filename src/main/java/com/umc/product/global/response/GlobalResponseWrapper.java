package com.umc.product.global.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestControllerAdvice
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    /**
     * beforeBodyWrite를 적용할지 여부를 결정
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 이미 ApiResponse로 래핑된 경우 제외
        Class<?> parameterType = returnType.getParameterType();
        if (ApiResponse.class.isAssignableFrom(parameterType)
                || ResponseEntity.class.isAssignableFrom(parameterType)
                || StreamingResponseBody.class.isAssignableFrom(parameterType)
                || byte[].class.isAssignableFrom(parameterType)) {
            return false;
        }

        if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }

        // Swagger/OpenAPI 관련 컨트롤러 제외
        String className = returnType.getContainingClass().getName();
        if (className.startsWith("org.springdoc") ||
                className.startsWith("org.springframework")) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // 이미 ApiResponse로 래핑된 경우 그대로 반환 (에러 응답 포함)
        if (body instanceof ApiResponse) {
            return body;
        }

        // String 타입은 별도 처리 필요 (MessageConverter 이슈)
        //
        if (body instanceof String) {
            return body;
        }

        return ApiResponse.onSuccess(body);
    }
}
