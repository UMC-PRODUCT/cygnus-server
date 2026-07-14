package com.umc.product.support.fixture;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.umc.product.organization.application.port.out.command.SaveGisuPort;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.support.CommonFixture;

@Component
public class GisuFixture extends FixtureSupport {

    private final SaveGisuPort saveGisuPort;

    private final Instant START_AT = Instant.parse("2024-03-01T00:00:00Z");
    private final Instant END_AT = Instant.parse("2024-08-31T23:59:59Z");

    public GisuFixture(SaveGisuPort saveGisuPort) {
        this.saveGisuPort = saveGisuPort;
    }

    public Gisu 활성_기수(Long generation) {
        return saveGisuPort.save(Gisu.create(
            generation,
            START_AT,
            END_AT,
            true
        ));
    }

    public Gisu 비활성_기수(Long generation) {
        return saveGisuPort.save(Gisu.create(
            generation,
            START_AT,
            END_AT,
            false
        ));
    }

    public Gisu 비활성_기수(Long generation, Instant startAt, Instant endAt) {
        return saveGisuPort.save(Gisu.create(generation, startAt, endAt, false));
    }

    public Gisu 활성_기수() {
        return 활성_기수(fixtureGeneration());
    }

    public Gisu 비활성_기수() {
        return 비활성_기수(fixtureGeneration());
    }

    private Long fixtureGeneration() {
        Long sampled = CommonFixture.MONKEY.giveMeOne(Long.class);
        long value = sampled != null ? sampled : 1L;
        return Math.floorMod(value, 10_000L) + 1;
    }
}
