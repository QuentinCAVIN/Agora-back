package com.agora.repository;

import com.agora.entity.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {
}
