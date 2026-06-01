package com.umc.product.global.security.util;

import com.umc.product.global.security.annotation.Public;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class PublicEndpointCollector {

    public static List<SecurityEndpoint> collectPublicEndpoints(RequestMappingHandlerMapping handlerMapping) {
        List<SecurityEndpoint> publicEndpoints = new ArrayList<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();

            // 메서드 또는 클래스에 @Public이 있는지 확인
            boolean isPublic = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Public.class) != null
                    || AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Public.class) != null;

            if (isPublic) {
                RequestMappingInfo mappingInfo = entry.getKey();
                Set<String> patterns = mappingInfo.getPatternValues();
                Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();

                // HTTP 메서드가 지정되지 않은 경우 모든 메서드 허용
                if (methods.isEmpty()) {
                    for (String pattern : patterns) {
                        publicEndpoints.add(SecurityEndpoint.any(pattern));
                    }
                } else {
                    // HTTP 메서드별로 패턴 추가
                    for (RequestMethod method : methods) {
                        HttpMethod httpMethod = HttpMethod.valueOf(method.name());
                        for (String pattern : patterns) {
                            publicEndpoints.add(SecurityEndpoint.of(httpMethod, pattern));
                        }
                    }
                }
            }
        }

        return publicEndpoints;
    }
}
