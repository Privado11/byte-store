package com.store.bytestore.dto;

import com.store.bytestore.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        UserRole role,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
