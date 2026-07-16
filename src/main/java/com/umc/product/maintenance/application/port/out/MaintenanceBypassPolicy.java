package com.umc.product.maintenance.application.port.out;

/**
 * 점검 중 차단을 우회할 수 있는 사용자를 결정하는 정책.
 */
public interface MaintenanceBypassPolicy {

    boolean shouldBypass(Long memberId);
}
