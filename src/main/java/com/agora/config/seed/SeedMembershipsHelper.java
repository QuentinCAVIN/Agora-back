package com.agora.config.seed;

import com.agora.entity.group.Group;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.repository.group.GroupMembershipRepository;

import java.time.Instant;

final class SeedMembershipsHelper {

    private final GroupMembershipRepository groupMembershipRepository;

    SeedMembershipsHelper(GroupMembershipRepository groupMembershipRepository) {
        this.groupMembershipRepository = groupMembershipRepository;
    }

    void ensureMembership(User user, Group group) {
        boolean exists = groupMembershipRepository.existsByUserIdAndGroupId(user.getId(), group.getId());
        if (exists) return;

        GroupMembership membership = new GroupMembership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setJoinedAt(Instant.now());
        groupMembershipRepository.save(membership);
    }
}

