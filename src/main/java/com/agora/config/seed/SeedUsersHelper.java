package com.agora.config.seed;

import com.agora.entity.user.ERole;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

final class SeedUsersHelper {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    SeedUsersHelper(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    SeededUsers ensureUsers() {
        User admin = ensureAutonomousUser(
                SeedConstants.EMAIL_ADMIN,
                "Admin",
                "Agora",
                "+221700000000",
                SeedConstants.DEFAULT_PASSWORD,
                ERole.SECRETARY_ADMIN,
                ERole.SUPERADMIN
        );

        User user = ensureAutonomousUser(
                SeedConstants.EMAIL_USER,
                "Jean",
                "Dupont",
                "0612345678",
                SeedConstants.DEFAULT_PASSWORD,
                ERole.CITIZEN
        );

        User staff = ensureAutonomousUser(
                SeedConstants.EMAIL_STAFF,
                "Marie",
                "Secrétaire",
                "0600000001",
                SeedConstants.DEFAULT_PASSWORD,
                ERole.CITIZEN
        );

        User assocManager = ensureAutonomousUser(
                SeedConstants.EMAIL_ASSOC_MANAGER,
                "Sophie",
                "Bernard",
                "0600000002",
                SeedConstants.DEFAULT_PASSWORD,
                ERole.SECRETARY_ADMIN
        );

        User tutored = ensureTutoredUser(
                "Germaine",
                "Perrier",
                "0556781234",
                SeedConstants.TUTORED_INTERNAL_REF_1,
                "Accompagnée par le CCAS"
        );

        return new SeededUsers(admin, user, staff, assocManager, tutored);
    }

    private User ensureAutonomousUser(
            String email,
            String firstName,
            String lastName,
            String phone,
            String rawPassword,
            ERole... roleNames
    ) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
        }

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);
        for (ERole roleName : roleNames) {
            user.addRole(roleName);
        }

        // internalRef/notesAdmin uniquement pour tutored
        user.setInternalRef(null);
        user.setNotesAdmin(null);

        return userRepository.save(user);
    }

    private User ensureTutoredUser(
            String firstName,
            String lastName,
            String phone,
            String internalRef,
            String notesAdmin
    ) {
        // Les comptes tutelle n'ont pas d'email : on utilise internalRef comme clé d'idempotence.
        User user = userRepository.findAll().stream()
                .filter(u -> internalRef.equals(u.getInternalRef()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            user = new User();
        }

        user.setEmail(null);
        user.setPasswordHash(null);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setAccountType(AccountType.TUTORED);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setInternalRef(internalRef);
        user.setNotesAdmin(notesAdmin);

        return userRepository.save(user);
    }

    record SeededUsers(
            User admin,
            User user,
            User staff,
            User assocManager,
            User tutored1
    ) {}
}

