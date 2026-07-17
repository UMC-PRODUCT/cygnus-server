package com.umc.product.registry.adapter.in.runner;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.umc.product.registry.domain.RegistryBackfillAction;

@ConfigurationProperties(prefix = "app.registry.backfill")
public class RegistryBackfillProperties {

    private RegistryBackfillAction action;
    private int batchSize = 500;
    private int detailLimit = 100;

    public RegistryBackfillAction getAction() {
        return action;
    }

    public void setAction(RegistryBackfillAction action) {
        this.action = action;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getDetailLimit() {
        return detailLimit;
    }

    public void setDetailLimit(int detailLimit) {
        this.detailLimit = detailLimit;
    }
}
