package com.agora.mapper.auth;

import com.agora.dto.response.auth.RegisterResponseDto;
import com.agora.dto.response.auth.UserSummaryDto;
import com.agora.entity.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public RegisterResponseDto toRegisterResponse(User user) {
        return new RegisterResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountType(),
                user.getAccountStatus()
        );
    }

    public UserSummaryDto toUserSummary(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountType(),
                user.getAccountStatus()
        );
    }
}
