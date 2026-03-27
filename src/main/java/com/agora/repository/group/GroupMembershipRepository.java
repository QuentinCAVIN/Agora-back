package com.agora.repository.group;

import com.agora.entity.group.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {

    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);

    @Query("""
            select gm
            from GroupMembership gm
            join fetch gm.group g
            where gm.user.id = :userId
            """)
    List<GroupMembership> findAllByUserIdWithGroup(UUID userId);
}
