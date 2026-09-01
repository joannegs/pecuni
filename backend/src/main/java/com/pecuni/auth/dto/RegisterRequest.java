package com.pecuni.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "email é obrigatório") @Email(message = "email deve ser válido") String email,
        @NotBlank(message = "senha é obrigatória")
                @Size(min = 8, max = 100, message = "senha deve ter entre 8 e 100 caracteres") String senha) {
}
