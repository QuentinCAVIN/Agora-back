package com.agora.config.seed;

import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.user.UserRepository;
import com.agora.service.reservation.ReservationBookingReferenceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationBookingReferenceService reservationBookingReferenceService;

    public DevSeedRunner(
            UserRepository userRepository,
            GroupRepository groupRepository,
            GroupMembershipRepository groupMembershipRepository,
            PasswordEncoder passwordEncoder,
            ResourceRepository resourceRepository,
            ReservationRepository reservationRepository,
            ReservationBookingReferenceService reservationBookingReferenceService
    ) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.reservationBookingReferenceService = reservationBookingReferenceService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        var groups = new SeedGroupsHelper(groupRepository).ensureGroups();
        var users = new SeedUsersHelper(userRepository, passwordEncoder).ensureUsers();
        var memberships = new SeedMembershipsHelper(groupMembershipRepository);

        memberships.ensureMembership(users.admin(), groups.publicGroup());
        memberships.ensureMembership(users.user(), groups.publicGroup());
        memberships.ensureMembership(users.staff(), groups.publicGroup());
        memberships.ensureMembership(users.assocManager(), groups.publicGroup());
        memberships.ensureMembership(users.tutored1(), groups.defaultGroup());

        memberships.ensureMembership(users.user(), groups.habitants());
        memberships.ensureMembership(users.assocManager(), groups.assoc());
        memberships.ensureMembership(users.staff(), groups.staff());
        memberships.ensureMembership(users.admin(), groups.council());

        new SeedResourcesHelper(resourceRepository).ensureResources();
        new SeedReservationsHelper(
                reservationRepository,
                resourceRepository,
                reservationBookingReferenceService
        ).ensureSeedReservations(users);
    }
}

