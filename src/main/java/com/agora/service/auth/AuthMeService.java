package com.agora.service.auth;

import com.agora.dto.response.auth.AuthMeResponseDto;
import com.agora.dto.response.auth.UserGroupSummaryDto;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.exception.auth.AuthUserNotFoundException;
import com.agora.config.SecurityUtils;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthMeService {

    private final UserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final SecurityUtils securityUtils;

    public AuthMeService(
            UserRepository userRepository,
            GroupMembershipRepository groupMembershipRepository,
            SecurityUtils securityUtils
    ) {
        this.userRepository = userRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public AuthMeResponseDto getCurrentUserProfile(Authentication authentication) {
        String authenticatedEmail = securityUtils.getAuthenticatedEmail(authentication);

        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new AuthUserNotFoundException(authenticatedEmail));

        List<UserGroupSummaryDto> groups = groupMembershipRepository.findAllByUserIdWithGroup(user.getId()).stream()
                .map(this::toGroupSummary)
                .toList();

        return new AuthMeResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountType(),
                user.getAccountStatus(),
                user.getPhone(),
                groups,
                user.getCreatedAt()
        );
    }

    private UserGroupSummaryDto toGroupSummary(GroupMembership membership) {
        return new UserGroupSummaryDto(
                membership.getGroup().getId(),
                membership.getGroup().getName(),
                membership.getGroup().isPreset()
        );
    }
}
