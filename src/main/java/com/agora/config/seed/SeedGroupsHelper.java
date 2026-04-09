package com.agora.config.seed;

import com.agora.entity.group.Group;
import com.agora.repository.group.GroupRepository;

final class SeedGroupsHelper {

    private final GroupRepository groupRepository;

    SeedGroupsHelper(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    SeededGroups ensureGroups() {
        Group publicGroup = groupRepository.findByName(SeedConstants.GROUP_PUBLIC).orElse(null);
        if (publicGroup == null) {
            throw new IllegalStateException("Le groupe preset 'Public' est introuvable (migration V2 attendue)");
        }

        Group defaultGroup = ensureGroup(SeedConstants.GROUP_DEFAULT, true);
        Group habitants = ensureGroup(SeedConstants.GROUP_HABITANTS, true);
        Group council = ensureGroup(SeedConstants.GROUP_COUNCIL, true);
        council.setCouncilPowers(true);
        council = groupRepository.save(council);
        Group assoc = ensureGroup(SeedConstants.GROUP_ASSOC, false);
        Group staff = ensureGroup(SeedConstants.GROUP_STAFF, false);

        return new SeededGroups(publicGroup, defaultGroup, habitants, council, assoc, staff);
    }

    private Group ensureGroup(String name, boolean preset) {
        Group group = groupRepository.findByName(name).orElse(null);
        if (group == null) {
            group = new Group();
            group.setName(name);
        }
        group.setPreset(preset);
        return groupRepository.save(group);
    }

    record SeededGroups(
            Group publicGroup,
            Group defaultGroup,
            Group habitants,
            Group council,
            Group assoc,
            Group staff
    ) {}
}

