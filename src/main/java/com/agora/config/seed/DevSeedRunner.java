package com.agora.config.seed;

import com.agora.entity.group.Group;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Seed DEV/LOCAL uniquement.
 *
 * - Idempotent (ré-exécutable)
 * - Ne dépend pas du JWT
 * - Fournit des comptes de test utilisables par le front via /api/auth/login
 */
@Profile({"dev", "local", "seed"})
@Component
public class DevSeedRunner implements CommandLineRunner {

    private static final String PUBLIC_GROUP_NAME = "Public";

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final PasswordEncoder passwordEncoder;

    public DevSeedRunner(
            UserRepository userRepository,
            GroupRepository groupRepository,
            GroupMembershipRepository groupMembershipRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Group publicGroup = groupRepository.findByName(PUBLIC_GROUP_NAME).orElse(null);
        if (publicGroup == null) {
            // Le groupe preset est censé être créé en migration V2.
            // En cas de DB déjà existante/incomplète, on préfère fail fast plutôt que créer un doublon.
            throw new IllegalStateException("Le groupe preset 'Public' est introuvable (migration V2 attendue)");
        }

        seedUser(publicGroup,
                "admin@agora.local",
                "Password123!",
                "Admin",
                "Agora",
                "+221700000000"
        );

        seedUser(publicGroup,
                "user@agora.local",
                "Password123!",
                "User",
                "Agora",
                "+221700000001"
        );
    }

    private void seedUser(
            Group publicGroup,
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            String phone
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

        User saved = userRepository.save(user);

        boolean alreadyMember = groupMembershipRepository.existsByUserIdAndGroupId(saved.getId(), publicGroup.getId());
        if (!alreadyMember) {
            GroupMembership membership = new GroupMembership();
            membership.setUser(saved);
            membership.setGroup(publicGroup);
            membership.setJoinedAt(Instant.now());
            groupMembershipRepository.save(membership);
        }
    }
}

