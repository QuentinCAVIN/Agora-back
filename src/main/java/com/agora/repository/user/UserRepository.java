package com.agora.repository.user;

import com.agora.entity.user.ERole;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /**
     * Résout l'utilisateur depuis le {@code sub} JWT : UUID (impersonation / sujet technique) ou email (connexion classique).
     */
    default Optional<User> findByJwtSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        String trimmed = subject.trim();
        try {
            UUID id = UUID.fromString(trimmed);
            return findByIdWithRoles(id).or(() -> findById(id));
        } catch (IllegalArgumentException ex) {
            return findByEmailIgnoreCase(trimmed);
        }
    }

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);

    @EntityGraph(attributePaths = "roles")
    @Query("""
            select distinct u
            from User u
            join u.roles role
            where role = :role
              and u.accountStatus = :accountStatus
            order by u.lastName asc, u.firstName asc, u.email asc
            """)
    List<User> findAllByRoleAndAccountStatus(
            @Param("role") ERole role,
            @Param("accountStatus") AccountStatus accountStatus
    );

    Optional<User> findByInternalRef(String internalRef);

    List<User> findAllByAdminSupportIsTrueAndAccountStatus(AccountStatus accountStatus);

    long countByAccountTypeAndAccountStatus(AccountType accountType, AccountStatus accountStatus);

    @Query("""
            select count(distinct u.id)
            from User u
            join u.roles r
            where r = :role
              and u.accountStatus = :accountStatus
            """)
    long countDistinctByRolesContainingAndAccountStatus(
            @Param("role") ERole role,
            @Param("accountStatus") AccountStatus accountStatus
    );
}
