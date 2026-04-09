package com.agora.service.group;

import com.agora.repository.group.GroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Appartenance à un groupe marqué {@link com.agora.entity.group.Group#isCouncilPowers()}.
 */
@Service
@RequiredArgsConstructor
public class CouncilMembershipService {

    private final GroupMembershipRepository groupMembershipRepository;

    @Transactional(readOnly = true)
    public boolean isCouncilMember(UUID userId) {
        if (userId == null) {
            return false;
        }
        return groupMembershipRepository.existsByUser_IdAndGroup_CouncilPowersIsTrue(userId);
    }
}
