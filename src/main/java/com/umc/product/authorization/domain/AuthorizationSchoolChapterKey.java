package com.umc.product.authorization.domain;

public record AuthorizationSchoolChapterKey(long gisuId, long schoolId) {
    public AuthorizationSchoolChapterKey {
        if (gisuId <= 0 || schoolId <= 0) {
            throw new IllegalArgumentException("Gisu와 School ID는 양수여야 합니다.");
        }
    }
}
