package com.agora.repository.group;

import com.agora.entity.group.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {

    long countByGroup_Id(UUID groupId);

    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);

    @Query("""
            select gm
            from GroupMembership gm
            join fetch gm.group g
            where gm.user.id = :userId
            """)
    List<GroupMembership> findAllByUserIdWithGroup(UUID userId);

    @Query("""
            select gm from GroupMembership gm
            join fetch gm.user u
            where gm.group.id = :groupId
            order by u.lastName asc, u.firstName asc
            """)
    List<GroupMembership> findAllByGroupIdWithUser(@Param("groupId") UUID groupId);

    @Query("""
            select gm from GroupMembership gm
            join fetch gm.group g
            where gm.user.id = :userId and g.id = :groupId
            """)
    Optional<GroupMembership> findByUserIdAndGroupIdWithGroup(
            @Param("userId") UUID userId,
            @Param("groupId") UUID groupId);

    Optional<GroupMembership> findByUser_IdAndGroup_Id(UUID userId, UUID groupId);

    void deleteByUser_Id(UUID userId);

    void deleteByGroup_Id(UUID groupId);
}
