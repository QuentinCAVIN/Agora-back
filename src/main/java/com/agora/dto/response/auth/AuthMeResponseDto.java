package com.agora.dto.response.auth;

import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthMeResponseDto {

    private final UUID id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final AccountType accountType;
    private final AccountStatus status;
    private final String phone;
    private final List<UserGroupSummaryDto> groups;
    private final Instant createdAt;
}
