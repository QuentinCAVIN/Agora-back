package com.agora.service.auth;

import com.agora.dto.response.auth.AuthMeResponseDto;
import com.agora.entity.group.Group;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.exception.auth.AuthUserNotFoundException;
import com.agora.config.SecurityUtils;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthMeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private SecurityUtils securityUtils;

    private AuthMeService authMeService;

    @BeforeEach
    void setUp() {
        authMeService = new AuthMeService(userRepository, groupMembershipRepository, securityUtils);
    }

    @Test
    void getCurrentUserProfile_nominal_returnsProfileWithGroups() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-26T10:15:30Z");

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPhone("0600000000");
        user.setCreatedAt(createdAt);

        Group group = new Group();
        group.setId(UUID.randomUUID());
        group.setName("Public");
        group.setPreset(true);

        GroupMembership membership = new GroupMembership();
        membership.setUser(user);
        membership.setGroup(group);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "ignored",
                List.of()
        );

        when(securityUtils.getAuthenticatedEmail(authentication)).thenReturn("user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(groupMembershipRepository.findAllByUserIdWithGroup(userId)).thenReturn(List.of(membership));

        AuthMeResponseDto response = authMeService.getCurrentUserProfile(authentication);

        assertEquals(userId, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Jane", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals(AccountType.AUTONOMOUS, response.getAccountType());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertEquals("0600000000", response.getPhone());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(1, response.getGroups().size());
        assertEquals("Public", response.getGroups().getFirst().getName());
        assertEquals(true, response.getGroups().getFirst().isPreset());
    }

    @Test
    void getCurrentUserProfile_userNotFound_throwsAuthUserNotFoundException() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "unknown@example.com",
                "ignored",
                List.of()
        );

        when(securityUtils.getAuthenticatedEmail(authentication)).thenReturn("unknown@example.com");
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthUserNotFoundException.class, () -> authMeService.getCurrentUserProfile(authentication));
    }
}
