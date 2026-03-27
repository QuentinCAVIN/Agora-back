package com.agora.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserSummaryDto user;
}
