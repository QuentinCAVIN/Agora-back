package com.agora.service.auth;

import com.agora.dto.request.auth.RegisterRequestDto;
import com.agora.dto.response.auth.RegisterResponseDto;
import com.agora.entity.group.Group;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.exception.auth.EmailAlreadyExistsException;
import com.agora.mapper.auth.UserMapper;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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
    void register_nominal_createsUserAndMembership_andHashesPassword() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("user@example.com");
        request.setPassword("SuperSecret123");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setPhone("0600000000");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SuperSecret123")).thenReturn("hashed");

        Group publicGroup = new Group();
        publicGroup.setId(UUID.randomUUID());
        publicGroup.setName("Public");
        publicGroup.setPreset(true);
        when(groupRepository.findByName("Public")).thenReturn(Optional.of(publicGroup));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        RegisterResponseDto response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("user@example.com", savedUser.getEmail());
        assertEquals("hashed", savedUser.getPasswordHash());
        assertEquals(AccountType.AUTONOMOUS, savedUser.getAccountType());
        assertEquals(AccountStatus.ACTIVE, savedUser.getAccountStatus());

        ArgumentCaptor<GroupMembership> membershipCaptor = ArgumentCaptor.forClass(GroupMembership.class);
        verify(groupMembershipRepository).save(membershipCaptor.capture());
        GroupMembership membership = membershipCaptor.getValue();

        assertNotNull(membership.getUser());
        assertEquals(publicGroup, membership.getGroup());
        assertNotNull(membership.getJoinedAt());

        assertNotNull(response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Jane", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals(AccountType.AUTONOMOUS, response.getAccountType());
        assertEquals(AccountStatus.ACTIVE, response.getAccountStatus());
    }

    @Test
    void register_emailAlreadyExists_throwsAndDoesNotCreateUser() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("user@example.com");
        request.setPassword("SuperSecret123");
        request.setFirstName("Jane");
        request.setLastName("Doe");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(groupMembershipRepository, never()).save(any(GroupMembership.class));
    }
}
