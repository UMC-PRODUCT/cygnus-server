package com.umc.product.demoday.application.port.out;

import com.umc.product.demoday.domain.DemodayStamp;

public interface SaveDemodayStampPort {

    DemodayStamp save(DemodayStamp stamp);
}
