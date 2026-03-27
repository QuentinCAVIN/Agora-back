package com.agora.service.auth;

import com.agora.dto.request.auth.LoginRequestDto;
import com.agora.dto.request.auth.RegisterRequestDto;
import com.agora.dto.response.auth.LoginResponseDto;
import com.agora.dto.response.auth.RegisterResponseDto;
import com.agora.entity.group.Group;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.exception.auth.AuthAccountNotAllowedException;
import com.agora.exception.auth.AuthInvalidCredentialsException;
import com.agora.exception.auth.EmailAlreadyExistsException;
import com.agora.mapper.auth.UserMapper;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private static final String PUBLIC_GROUP_NAME = "Public";

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(
            UserRepository userRepository,
            GroupRepository groupRepository,
            GroupMembershipRepository groupMembershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        String email = request.getEmail().trim();

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }

        Group publicGroup = groupRepository.findByName(PUBLIC_GROUP_NAME)
                .orElseThrow(() -> new IllegalStateException("Le groupe preset 'Public' est introuvable"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(request.getPhone());
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        GroupMembership membership = new GroupMembership();
        membership.setUser(savedUser);
        membership.setGroup(publicGroup);
        membership.setJoinedAt(Instant.now());
        groupMembershipRepository.save(membership);

        return userMapper.toRegisterResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String email = request.getEmail().trim();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(AuthInvalidCredentialsException::new);

        if (user.getAccountType() != AccountType.AUTONOMOUS) {
            throw new AuthAccountNotAllowedException("Ce compte n'est pas autorisé à se connecter");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthAccountNotAllowedException("Ce compte n'est pas autorisé à se connecter");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new AuthInvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthInvalidCredentialsException();
        }

        String token = jwtService.generateAccessToken(user);

        return new LoginResponseDto(
                token,
                "Bearer",
                jwtService.getExpiresInSeconds(),
                userMapper.toUserSummary(user)
        );
    }
}
