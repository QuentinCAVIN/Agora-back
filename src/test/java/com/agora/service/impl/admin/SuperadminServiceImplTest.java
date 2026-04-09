package com.agora.service.impl.admin;

import com.agora.dto.response.admin.AdminSupportUserDto;
import com.agora.entity.user.ERole;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperadminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SuperadminServiceImpl superadminService;

    @Test
    void getActiveAdminSupportUsers_shouldReturnMappedUsers() {
        User user = buildUser("paul.assiste@mairie.fr", AccountStatus.ACTIVE);
        user.addRole(ERole.DELEGATE_ADMIN);

        when(userRepository.findAllByRoleAndAccountStatus(ERole.DELEGATE_ADMIN, AccountStatus.ACTIVE))
                .thenReturn(List.of(user));

        List<AdminSupportUserDto> result = superadminService.getActiveAdminSupportUsers();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().email()).isEqualTo("paul.assiste@mairie.fr");
        assertThat(result.getFirst().status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void grantAdminSupport_shouldAddDelegateAdminRole() {
        User user = buildUser("jean.dupont@gmail.com", AccountStatus.ACTIVE);

        when(userRepository.findByIdWithRoles(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        AdminSupportUserDto result = superadminService.grantAdminSupport(user.getId());

        assertThat(user.getRoles()).contains(ERole.DELEGATE_ADMIN);
        assertThat(result.email()).isEqualTo("jean.dupont@gmail.com");
        verify(userRepository).save(user);
    }

    @Test
    void grantAdminSupport_shouldThrowConflictWhenAlreadyAdminSupport() {
        User user = buildUser("jean.dupont@gmail.com", AccountStatus.ACTIVE);
        user.addRole(ERole.DELEGATE_ADMIN);

        when(userRepository.findByIdWithRoles(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> superadminService.grantAdminSupport(user.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.ADMIN_SUPPORT_ALREADY_EXISTS);
    }

    @Test
    void grantAdminSupport_shouldThrowValidationWhenUserHasNoEmail() {
        User user = buildUser(null, AccountStatus.ACTIVE);

        when(userRepository.findByIdWithRoles(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> superadminService.grantAdminSupport(user.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void revokeAdminSupport_shouldRemoveDelegateAdminRole() {
        User user = buildUser("jean.dupont@gmail.com", AccountStatus.ACTIVE);
        user.addRole(ERole.DELEGATE_ADMIN);

        when(userRepository.findByIdWithRoles(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        superadminService.revokeAdminSupport(user.getId());

        assertThat(user.getRoles()).doesNotContain(ERole.DELEGATE_ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void revokeAdminSupport_shouldBeNoOpWhenRoleMissing() {
        User user = buildUser("jean.dupont@gmail.com", AccountStatus.ACTIVE);

        when(userRepository.findByIdWithRoles(user.getId())).thenReturn(Optional.of(user));

        superadminService.revokeAdminSupport(user.getId());

        verify(userRepository, never()).save(user);
    }

    private static User buildUser(String email, AccountStatus accountStatus) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        user.setAccountStatus(accountStatus);
        return user;
    }
}
