package com.umc.product.storage.application.authorization;

public enum StoragePolicyAction {
    DELETE_FILE("storage-file:delete");

    private final String id;

    StoragePolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
