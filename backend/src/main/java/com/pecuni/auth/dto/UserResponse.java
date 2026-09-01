package com.pecuni.auth.dto;

import com.pecuni.user.AuthProvider;
import java.util.UUID;

public record UserResponse(UUID id, String nome, String email, AuthProvider provider) {
}
