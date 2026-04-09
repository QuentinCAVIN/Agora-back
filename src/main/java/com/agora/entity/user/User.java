package com.agora.entity.user;

import com.agora.entity.common.Auditable;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor

public class User extends Auditable {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 50)
    private AccountStatus accountStatus;

    @Column(name = "internal_ref", unique = true, length = 100)
    private String internalRef;

    @Column(name = "notes_admin", columnDefinition = "TEXT")
    private String notesAdmin;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "roles", nullable = false)
    private Set<ERole> roles = new HashSet<>();

    public void addRole(ERole role) {
        if(this.roles == null) this.roles = new HashSet<>();
        this.roles.add(role);
    }
}
