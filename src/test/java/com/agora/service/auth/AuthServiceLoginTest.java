package com.agora.service.auth;

import com.agora.dto.request.auth.LoginRequestDto;
import com.agora.dto.response.auth.LoginResponseDto;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.exception.auth.AuthAccountNotAllowedException;
import com.agora.exception.auth.AuthInvalidCredentialsException;
import com.agora.mapper.auth.UserMapper;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                groupRepository,
                groupMembershipRepository,
                passwordEncoder,
                jwtService,
                new UserMapper()
        );
    }

    @Test
    void login_nominal_returnsJwt_andUserSummary() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("user@example.com");
        request.setPassword("SuperSecret123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SuperSecret123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-jwt");
        when(jwtService.getExpiresInSeconds()).thenReturn(3600L);

        LoginResponseDto response = authService.login(request).response();

        assertEquals("jwt", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(user.getId(), response.getUser().getId());
        assertEquals("Jane", response.getUser().getFirstName());
        assertEquals("Doe", response.getUser().getLastName());
        assertEquals(AccountType.AUTONOMOUS, response.getUser().getAccountType());
        assertEquals(AccountStatus.ACTIVE, response.getUser().getAccountStatus());
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("unknown@example.com");
        request.setPassword("SuperSecret123");

        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthInvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_invalidPassword_throwsInvalidCredentials() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("user@example.com");
        request.setPassword("bad");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        assertThrows(AuthInvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_tutoredAccount_refused() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("user@example.com");
        request.setPassword("SuperSecret123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setAccountType(AccountType.TUTORED);
        user.setAccountStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(AuthAccountNotAllowedException.class, () -> authService.login(request));
    }

    @Test
    void login_deletedAccount_refused() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("user@example.com");
        request.setPassword("SuperSecret123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.DELETED);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(AuthAccountNotAllowedException.class, () -> authService.login(request));
    }
}
