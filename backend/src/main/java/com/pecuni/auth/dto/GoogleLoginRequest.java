package com.pecuni.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(@NotBlank(message = "idToken é obrigatório") String idToken) {
}
