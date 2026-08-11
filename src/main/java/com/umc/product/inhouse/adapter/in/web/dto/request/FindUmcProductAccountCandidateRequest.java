package com.umc.product.inhouse.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FindUmcProductAccountCandidateRequest(
    @NotBlank @Email @Size(max = 100) String email
) {
}
