package com.umc.product.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GeometryUtils")
class GeometryUtilsTest {

    @Test
    @DisplayName("위도·경도를 SRID 4326의 longitude/latitude Point로 변환한다")
    void 좌표를_Point로_변환한다() {
        var point = GeometryUtils.createPoint(37.5, 127.0);

        assertThat(point.getX()).isEqualTo(127.0);
        assertThat(point.getY()).isEqualTo(37.5);
        assertThat(point.getSRID()).isEqualTo(4326);
    }

    @Test
    @DisplayName("위도 또는 경도가 null이면 부분 좌표를 만들지 않는다")
    void null_좌표는_생성하지_않는다() {
        assertThat(GeometryUtils.createPoint(null, 127.0)).isNull();
        assertThat(GeometryUtils.createPoint(37.5, null)).isNull();
    }
}
