package com.umc.product.global.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.response.code.SuccessCode;

@DisplayName("global response 잔여 계약")
class ResponseResidualTest {

    @Test
    @DisplayName("PageResponse는 원본 page metadata와 mapper 적용 결과를 보존한다")
    void PageResponse를_변환한다() {
        PageImpl<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(1, 2), 5);

        PageResponse<Integer> original = PageResponse.of(page);
        PageResponse<String> mapped = PageResponse.of(page, Object::toString);

        assertThat(original.content()).containsExactly(1, 2);
        assertThat(mapped.content()).containsExactly("1", "2");
        assertThat(mapped.page()).isOne();
        assertThat(mapped.totalElements()).isEqualTo(5L);
        assertThat(mapped.hasNext()).isTrue();
        assertThat(mapped.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("CursorResponse는 초과 row를 제거하고 순서·next cursor·마지막 page를 계산한다")
    void CursorResponse를_계산한다() {
        CursorResponse<String> next = CursorResponse.of(List.of(1L, 2L, 3L), 2, value -> value, Object::toString);
        CursorResponse<String> last = CursorResponse.of(List.of(1L, 2L), 2, value -> value, Object::toString);
        CursorResponse<Long> zeroSize = CursorResponse.of(List.of(1L), 0, value -> value, value -> value);

        assertThat(next.content()).containsExactly("1", "2");
        assertThat(next.nextCursor()).isEqualTo(2L);
        assertThat(next.hasNext()).isTrue();
        assertThat(last.nextCursor()).isNull();
        assertThat(last.hasNext()).isFalse();
        assertThat(zeroSize.content()).isEmpty();
        assertThat(zeroSize.nextCursor()).isNull();
        assertThat(CursorResponse.of(List.of("a"), 1L, true).nextCursor()).isOne();
        assertThat(CursorResponse.empty().content()).isEmpty();
    }

    @Test
    @DisplayName("공통 PageRequest는 1-based page를 안전한 long offset으로 변환한다")
    void PageRequest_offset을_계산한다() {
        assertThat(new com.umc.product.global.response.PageRequest(3, 20).offset()).isEqualTo(40L);
    }

    @Test
    @DisplayName("ApiResponse와 error factory는 기본·사용자 메시지와 상세 정보를 보존한다")
    void ApiResponse와_error_factory를_검증한다() {
        ApiResponse<String> success = ApiResponse.onSuccess("value");
        ApiResponse<String> failure = ApiResponse.onFailure("CODE", "message", "detail");

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getCode()).isEqualTo(SuccessCode._OK.getCode());
        assertThat(failure.isSuccess()).isFalse();
        assertThat(ApiErrorResponseFactory.from(CommonErrorCode.BAD_REQUEST).getMessage())
            .isEqualTo(CommonErrorCode.BAD_REQUEST.getMessage());
        assertThat(ApiErrorResponseFactory.from(CommonErrorCode.BAD_REQUEST, "detail").getResult())
            .isEqualTo("detail");
        assertThat(ApiErrorResponseFactory.from(CommonErrorCode.BAD_REQUEST, "custom", "detail").getMessage())
            .isEqualTo("custom");
        assertThat(ApiErrorResponseFactory.resolveMessage(CommonErrorCode.BAD_REQUEST, " "))
            .isEqualTo(CommonErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    @DisplayName("error writer의 세 overload는 status·UTF-8 JSON envelope를 동일하게 기록한다")
    void ApiErrorResponseWriter_overload를_검증한다() throws Exception {
        ApiErrorResponseWriter sut = new ApiErrorResponseWriter(new ObjectMapper());
        MockHttpServletResponse basic = new MockHttpServletResponse();
        MockHttpServletResponse detail = new MockHttpServletResponse();
        MockHttpServletResponse custom = new MockHttpServletResponse();

        sut.write(basic, CommonErrorCode.BAD_REQUEST);
        sut.write(detail, CommonErrorCode.NOT_FOUND, "missing-id");
        sut.write(custom, CommonErrorCode.FORBIDDEN, "custom", "detail");

        assertThat(basic.getStatus()).isEqualTo(400);
        assertThat(basic.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(basic.getContentAsString()).contains("COMMON-400");
        assertThat(detail.getContentAsString()).contains("missing-id");
        assertThat(custom.getContentAsString()).contains("custom", "detail");
    }

    @Test
    @DisplayName("전역 wrapper는 이미 제어되는 응답 타입과 Spring 내부 controller를 제외한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void GlobalResponseWrapper_제외_타입을_검증한다() throws Exception {
        GlobalResponseWrapper sut = new GlobalResponseWrapper();

        assertThat(sut.supports(returnType("api"), MappingJackson2HttpMessageConverter.class)).isFalse();
        assertThat(sut.supports(returnType("entity"), MappingJackson2HttpMessageConverter.class)).isFalse();
        assertThat(sut.supports(returnType("stream"), MappingJackson2HttpMessageConverter.class)).isFalse();
        assertThat(sut.supports(returnType("bytes"), MappingJackson2HttpMessageConverter.class)).isFalse();
        MethodParameter springType = mock(MethodParameter.class);
        org.mockito.Mockito.doReturn(String.class).when(springType).getParameterType();
        given(springType.getContainingClass()).willReturn((Class)ResponseEntity.class);
        assertThat(sut.supports(springType, MappingJackson2HttpMessageConverter.class)).isFalse();
    }

    @Test
    @DisplayName("body wrapper는 기존 envelope와 String을 보존하고 일반 DTO/null만 성공 envelope로 감싼다")
    void GlobalResponseWrapper_body를_변환한다() throws Exception {
        GlobalResponseWrapper sut = new GlobalResponseWrapper();
        ApiResponse<String> envelope = ApiResponse.onSuccess("ok");
        MethodParameter returnType = returnType("dto");
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        assertThat(write(sut, envelope, returnType, request, response)).isSameAs(envelope);
        assertThat(write(sut, "plain", returnType, request, response)).isEqualTo("plain");
        assertThat(write(sut, new Sample("value"), returnType, request, response))
            .isInstanceOf(ApiResponse.class);
        assertThat(((ApiResponse<?>)write(sut, null, returnType, request, response)).getResult()).isNull();
    }

    private Object write(
        GlobalResponseWrapper sut,
        Object body,
        MethodParameter returnType,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        return sut.beforeBodyWrite(
            body, returnType, MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class, request, response);
    }

    private MethodParameter returnType(String name) throws Exception {
        Method method = Fixture.class.getDeclaredMethod(name);
        return new MethodParameter(method, -1);
    }

    private static final class Fixture {
        ApiResponse<String> api() { return null; }
        ResponseEntity<String> entity() { return null; }
        StreamingResponseBody stream() { return null; }
        byte[] bytes() { return null; }
        Sample dto() { return null; }
    }

    private record Sample(String value) { }
}
