package com.umc.product.demoday.application.port.out;

import java.util.Map;

public interface LoadDemodayDashboardPort {

    /**
     * Poll에 속한 부스별 유효(무효 처리되지 않은) 득표 수를 집계한다.
     *
     * <p>득표가 하나도 없는 부스는 이 맵에 나타나지 않는다. 0표 부스까지 포함한 전체 목록을 만드는 책임은
     * 호출자(부스 전체 목록을 아는 쪽)에게 있다.
     */
    Map<Long, Long> countActiveVotesByBooth(Long pollId);

    /**
     * Poll에 속한 부스별 유효(무효 처리되지 않은) 스탬프 수를 집계한다.
     *
     * <p>스탬프가 하나도 없는 부스는 이 맵에 나타나지 않는다. 0개 부스까지 포함한 전체 목록을 만드는 책임은
     * 호출자에게 있다.
     */
    Map<Long, Long> countActiveStampsByBooth(Long pollId);
}
