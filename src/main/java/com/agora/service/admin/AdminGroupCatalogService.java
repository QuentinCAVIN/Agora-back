package com.agora.service.admin;

import com.agora.dto.request.admin.AddGroupMemberRequestDto;
import com.agora.dto.request.admin.CreateAdminGroupRequestDto;
import com.agora.dto.request.admin.UpdateAdminGroupRequestDto;
import com.agora.dto.response.admin.AdminGroupMemberResponseDto;
import com.agora.dto.response.admin.AdminGroupResponseDto;
import com.agora.dto.response.auth.GroupDiscountLabelFormatter;
import com.agora.entity.group.Group;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.User;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.config.SecurityUtils;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.user.UserRepository;
import com.agora.service.impl.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminGroupCatalogService {

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AdminGroupResponseDto> listGroups() {
        return groupRepository.findAll().stream().map(this::toGroupDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminGroupResponseDto getGroup(UUID groupId) {
        Group g = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Groupe introuvable"));
        return toGroupDto(g);
    }

    @Transactional
    public AdminGroupResponseDto createGroup(CreateAdminGroupRequestDto request) {
        if (groupRepository.findByName(request.name().trim()).isPresent()) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Un groupe avec ce nom existe déjà.");
        }

        Group group = new Group();
        group.setPreset(false);
        applyGroupFields(group, request.name().trim(), request);
        Group saved = groupRepository.save(group);
        String actor = securityUtils.tryGetAuthenticatedEmail().orElse("SYSTEM");
        auditService.log(
                "ADMIN_GROUP_CREATED",
                actor,
                null,
                Map.of("groupId", saved.getId().toString(), "name", saved.getName()),
                false
        );
        return toGroupDto(saved);
    }

    @Transactional
    public AdminGroupResponseDto updateGroup(UUID groupId, UpdateAdminGroupRequestDto request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Groupe introuvable"));
        if (group.isPreset()) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Impossible de modifier un groupe prédéfini.");
        }
        groupRepository.findByName(request.name().trim())
                .filter(g -> !g.getId().equals(groupId))
                .ifPresent(g -> {
                    throw new BusinessException(ErrorCode.DATA_CONFLICT, "Un groupe avec ce nom existe déjà.");
                });
        applyGroupFields(group, request.name().trim(), request);
        Group saved = groupRepository.save(group);
        String actor = securityUtils.tryGetAuthenticatedEmail().orElse("SYSTEM");
        auditService.log(
                "ADMIN_GROUP_UPDATED",
                actor,
                null,
                Map.of("groupId", groupId.toString(), "name", saved.getName()),
                false
        );
        return toGroupDto(saved);
    }

    private void applyGroupFields(Group group, String name, CreateAdminGroupRequestDto request) {
        group.setName(name);
        group.setCanBookImmobilier(request.canBookImmobilier());
        group.setCanBookMobilier(request.canBookMobilier());
        group.setDiscountType(request.discountType());
        group.setDiscountValue(request.discountValue());
        group.setDiscountAppliesTo(request.discountAppliesTo());
    }

    private void applyGroupFields(Group group, String name, UpdateAdminGroupRequestDto request) {
        group.setName(name);
        group.setCanBookImmobilier(request.canBookImmobilier());
        group.setCanBookMobilier(request.canBookMobilier());
        group.setDiscountType(request.discountType());
        group.setDiscountValue(request.discountValue());
        group.setDiscountAppliesTo(request.discountAppliesTo());
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Groupe introuvable"));
        if (group.isPreset()) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Impossible de supprimer un groupe prédéfini.");
        }
        String actor = securityUtils.tryGetAuthenticatedEmail().orElse("SYSTEM");
        auditService.log(
                "ADMIN_GROUP_DELETED",
                actor,
                group.getName(),
                Map.of("groupId", groupId.toString(), "name", group.getName()),
                false
        );
        groupMembershipRepository.deleteByGroup_Id(groupId);
        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public List<AdminGroupMemberResponseDto> listMembers(UUID groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Groupe introuvable"));

        return groupMembershipRepository.findAllByGroupIdWithUser(groupId).stream()
                .map(this::toMemberDto)
                .toList();
    }

    @Transactional
    public void addMember(UUID groupId, AddGroupMemberRequestDto body) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Groupe introuvable"));
        User user = userRepository.findById(body.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Utilisateur introuvable"));
        if (groupMembershipRepository.existsByUserIdAndGroupId(user.getId(), group.getId())) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Le membre est déjà dans ce groupe.");
        }
        GroupMembership gm = new GroupMembership();
        gm.setUser(user);
        gm.setGroup(group);
        gm.setJoinedAt(Instant.now());
        groupMembershipRepository.save(gm);
        String actor = securityUtils.tryGetAuthenticatedEmail().orElse("SYSTEM");
        auditService.log(
                "ADMIN_GROUP_MEMBER_ADDED",
                actor,
                userLabel(user),
                Map.of("groupId", groupId.toString(), "userId", user.getId().toString()),
                false
        );
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Groupe introuvable"));
        GroupMembership gm = groupMembershipRepository.findByUser_IdAndGroup_Id(userId, groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Adhésion introuvable"));
        User member = gm.getUser();
        String actor = securityUtils.tryGetAuthenticatedEmail().orElse("SYSTEM");
        auditService.log(
                "ADMIN_GROUP_MEMBER_REMOVED",
                actor,
                userLabel(member),
                Map.of("groupId", groupId.toString(), "userId", userId.toString()),
                false
        );
        groupMembershipRepository.delete(gm);
    }

    private static String userLabel(User u) {
        if (u.getEmail() != null && !u.getEmail().isBlank()) {
            return u.getEmail();
        }
        if (u.getInternalRef() != null && !u.getInternalRef().isBlank()) {
            return u.getInternalRef();
        }
        return u.getId().toString();
    }

    private AdminGroupResponseDto toGroupDto(Group g) {
        int count = (int) groupMembershipRepository.countByGroup_Id(g.getId());
        return new AdminGroupResponseDto(
                g.getId().toString(),
                g.getName(),
                g.isPreset(),
                g.isCanBookImmobilier(),
                g.isCanBookImmobilier(),
                g.isCanBookMobilier(),
                g.isCanBookMobilier(),
                g.getDiscountType(),
                g.getDiscountValue(),
                g.getDiscountAppliesTo(),
                GroupDiscountLabelFormatter.format(g.getDiscountType(), g.getDiscountValue()),
                count,
                g.isCouncilPowers()
        );
    }

    private AdminGroupMemberResponseDto toMemberDto(GroupMembership gm) {
        var u = gm.getUser();
        return new AdminGroupMemberResponseDto(
                u.getId().toString(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                "MEMBER",
                gm.getJoinedAt().toString()
        );
    }
}
