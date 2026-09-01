package com.pecuni.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Also reused as the request body for {@code POST /auth/logout} — same shape, one field. */
public record RefreshRequest(@NotBlank(message = "refreshToken é obrigatório") String refreshToken) {
}
