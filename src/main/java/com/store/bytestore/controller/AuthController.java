package com.store.bytestore.controller;

import com.store.bytestore.dto.LoginRequest;
import com.store.bytestore.dto.RefreshTokenRequest;
import com.store.bytestore.dto.TokenDto;
import com.store.bytestore.dto.UserDto;
import com.store.bytestore.dto.UserToSaveDto;
import com.store.bytestore.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody @Valid UserToSaveDto userToSaveDto) {
        UserDto saved = authService.createUser(userToSaveDto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody @Valid LoginRequest loginRequest) {
        TokenDto tokenDto = authService.login(loginRequest);
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenDto> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        TokenDto tokenDto = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}
