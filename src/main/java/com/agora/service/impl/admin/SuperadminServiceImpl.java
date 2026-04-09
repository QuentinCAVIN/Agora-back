package com.agora.service.impl.admin;

import com.agora.config.Audited;
import com.agora.dto.response.admin.AdminSupportUserDto;
import com.agora.entity.user.ERole;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.repository.user.UserRepository;
import com.agora.service.admin.SuperadminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperadminServiceImpl implements SuperadminService {

    private static final ERole ADMIN_SUPPORT_ROLE = ERole.DELEGATE_ADMIN;

    private final UserRepository userRepository;

    @Override
    public List<AdminSupportUserDto> getActiveAdminSupportUsers() {
        return userRepository.findAllByRoleAndAccountStatus(ADMIN_SUPPORT_ROLE, AccountStatus.ACTIVE).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    @Audited(action = "ADMIN_SUPPORT_GRANTED")
    public AdminSupportUserDto grantAdminSupport(UUID userId) {
        User user = loadUser(userId);
        validateGrantTarget(user);

        if (user.getRoles().contains(ADMIN_SUPPORT_ROLE) || user.getRoles().contains(ERole.SECRETARY_ADMIN)) {
            throw new BusinessException(
                    ErrorCode.ADMIN_SUPPORT_ALREADY_EXISTS,
                    "Cet utilisateur est déjà ADMIN_SUPPORT"
            );
        }

        user.addRole(ADMIN_SUPPORT_ROLE);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    @Audited(action = "ADMIN_SUPPORT_REVOKED")
    public void revokeAdminSupport(UUID userId) {
        User user = loadUser(userId);
        if (user.getRoles().remove(ADMIN_SUPPORT_ROLE)) {
            userRepository.save(user);
        }
    }

    private User loadUser(UUID userId) {
        return userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Utilisateur introuvable"
                ));
    }

    private void validateGrantTarget(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Seuls les utilisateurs actifs peuvent devenir ADMIN_SUPPORT"
            );
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Le compte doit avoir un email pour devenir ADMIN_SUPPORT"
            );
        }

        if (user.getRoles().contains(ERole.SUPERADMIN)) {
            throw new BusinessException(
                    ErrorCode.DATA_CONFLICT,
                    "Un SUPERADMIN ne peut pas être promu ADMIN_SUPPORT"
            );
        }
    }

    private AdminSupportUserDto toDto(User user) {
        return new AdminSupportUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountStatus()
        );
    }
}
