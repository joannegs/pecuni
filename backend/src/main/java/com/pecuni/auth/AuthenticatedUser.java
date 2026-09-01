package com.pecuni.auth;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID workspaceId, String email) {
}
