package com.store.bytestore.services;

import com.store.bytestore.dto.LoginRequest;
import com.store.bytestore.dto.TokenDto;
import com.store.bytestore.dto.UserDto;
import com.store.bytestore.dto.UserToSaveDto;

public interface AuthService {
    UserDto createUser(UserToSaveDto userToSaveDto);
    TokenDto login(LoginRequest loginRequest);
    TokenDto refreshToken(String refreshToken);
    void logout(String accessToken);
}
