package com.agora.service.auth;

import com.agora.dto.response.auth.AuthMeResponseDto;
import com.agora.dto.response.auth.UserGroupSummaryDto;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.exception.auth.AuthRequiredException;
import com.agora.exception.auth.AuthUserNotFoundException;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.user.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthMeService {

    private final UserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    public AuthMeService(UserRepository userRepository, GroupMembershipRepository groupMembershipRepository) {
        this.userRepository = userRepository;
        this.groupMembershipRepository = groupMembershipRepository;
    }

    @Transactional(readOnly = true)
    public AuthMeResponseDto getCurrentUserProfile(Authentication authentication) {
        String authenticatedEmail = extractAuthenticatedEmail(authentication);

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

    private String extractAuthenticatedEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthRequiredException();
        }

        // TODO(JWT): contrat transitoire J2-06.
        // L'identite courante est resolue via Authentication#getName().
        // Cela suppose que le principal expose l'email utilisateur.
        // A realigner lors du branchement JWT complet si le principal evolue (sub/id custom).
        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new AuthRequiredException();
        }

        return email.trim();
    }

    private UserGroupSummaryDto toGroupSummary(GroupMembership membership) {
        return new UserGroupSummaryDto(
                membership.getGroup().getId(),
                membership.getGroup().getName(),
                membership.getGroup().isPreset()
        );
    }
}
