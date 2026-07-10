package com.umc.product.member.application.port.in.query;

public interface CheckMemberExistenceUseCase {

    boolean existsById(Long memberId);
}
