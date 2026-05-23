package com.store.bytestore.services;

import com.store.bytestore.dto.*;
import com.store.bytestore.entities.User;
import com.store.bytestore.entities.UserToken;
import com.store.bytestore.enums.TokenType;
import com.store.bytestore.reposiroty.UserRepository;
import com.store.bytestore.reposiroty.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserTokenRepository userTokenRepository;

    @Override
    public UserDto createUser(UserToSaveDto userToSaveDto) {
        User user = userMapper.UserToSaveDtoToEntity(userToSaveDto);
        user.setPasswordHash(
                passwordEncoder.encode(userToSaveDto.password())
        );
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public TokenDto login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken, TokenType.BEARER, jwtService.getAccessExpirationMs());
        saveUserToken(user, refreshToken, TokenType.REFRESH, jwtService.getRefreshExpirationMs());

        return new TokenDto(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public TokenDto refreshToken(String refreshToken) {
        String userEmail = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserToken storedRefreshToken = userTokenRepository
                .findByTokenAndType(refreshToken, TokenType.REFRESH)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (storedRefreshToken.getRevoked() || jwtService.isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        // Revocar el access token anterior
        userTokenRepository.deleteByUserIdAndType(user.getId(), TokenType.BEARER);

        // Generar nuevo access token
        String newAccessToken = jwtService.generateToken(user);
        saveUserToken(user, newAccessToken, TokenType.BEARER, jwtService.getAccessExpirationMs());

        return new TokenDto(newAccessToken, refreshToken);
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        UserToken storedToken = userTokenRepository
                .findByToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        User user = storedToken.getUser();
        revokeAllUserTokens(user);
    }

    private void saveUserToken(User user, String token, TokenType type, long expirationMs) {
        UserToken userToken = UserToken.builder()
                .user(user)
                .token(token)
                .type(type)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusNanos(expirationMs * 1_000_000))
                .build();
        userTokenRepository.save(userToken);
    }

    private void revokeAllUserTokens(User user) {
        var validTokens = userTokenRepository.findAllValidTokensByUser(user.getId());
        for (UserToken token : validTokens) {
            token.setRevoked(true);
        }
        userTokenRepository.saveAll(validTokens);
    }
}
