package com.store.bytestore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserToSaveDto(
        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Email
        String email,

        @Size(max = 20)
        String phone,

        @NotBlank
        @Size(min = 8, max = 100)
        String password

) {
}