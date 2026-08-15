package com.umc.product.demoday.application.port.out;

import java.util.List;

import com.umc.product.demoday.domain.DemodayBooth;

public interface SaveDemodayBoothPort {

    DemodayBooth save(DemodayBooth booth);

    List<DemodayBooth> saveAll(List<DemodayBooth> booths);
}
