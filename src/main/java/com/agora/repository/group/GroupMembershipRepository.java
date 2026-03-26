package com.agora.repository.group;

import com.agora.entity.group.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {

    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);
}
