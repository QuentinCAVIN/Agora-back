package com.agora.service.impl.resource;

import com.agora.dto.request.resource.ResourceRequest;
import com.agora.entity.resource.Resource;
import com.agora.enums.resource.ResourceType;
import com.agora.exception.resource.ResourceNotFountException;
import com.agora.mapper.resource.ResourceMapper;
import com.agora.repository.resource.ResourceRepository;
import com.agora.service.impl.resource.ResourceServiceImpl;
import com.agora.testutil.ResourceTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ResourceServiceImplTest {

    @Mock
    private ResourceRepository repository;

    @Mock
    private ResourceMapper mapper;

    @InjectMocks
    private ResourceServiceImpl service;

    // =========================================
    // CREATE
    // =========================================
    @Test
    void createResource_shouldSaveAndReturnDto() {

        ResourceRequest request = new ResourceRequest(
                "Salle",
                ResourceType.IMMOBILIER,
                100,
                "desc",
                15000,
                ResourceTestData.meetingRoomImage(),
                List.of("PMR")
        );

        Resource entity = new Resource();
        Resource saved = new Resource();
        saved.setId(UUID.randomUUID());

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(
                new com.agora.dto.response.resource.ResourceDto(
                        saved.getId(), "Salle", ResourceType.IMMOBILIER,
                        100, "desc", 15000,
                        ResourceTestData.meetingRoomImage(),
                        List.of("PMR"), true
                )
        );

        var result = service.createResource(request);

        assertThat(result).isNotNull();
        verify(repository).save(entity);
    }

    // =========================================
    // GET BY ID
    // =========================================
    @Test
    void getResourceById_shouldThrow_whenNotFound() {

        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResourceById(id))
                .isInstanceOf(ResourceNotFountException.class);
    }

    // =========================================
    // UPDATE
    // =========================================
    @Test
    void updateResource_shouldUpdate() {

        UUID id = UUID.randomUUID();

        Resource existing = new Resource();
        existing.setId(id);

        ResourceRequest request = new ResourceRequest(
                "New",
                ResourceType.IMMOBILIER,
                200,
                "desc",
                10000,
                ResourceTestData.randomImage(),
                List.of()
        );

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(
                new com.agora.dto.response.resource.ResourceDto(
                        id, "New", ResourceType.IMMOBILIER,
                        200, "desc", 10000,
                        ResourceTestData.randomImage(),
                        List.of(), true
                )
        );

        var result = service.updateResource(id, request);

        assertThat(result).isNotNull();
        verify(mapper).updateEntity(existing, request);
    }

    // =========================================
    // DELETE
    // =========================================
    @Test
    void deleteResource_shouldSoftDelete() {

        UUID id = UUID.randomUUID();

        Resource resource = new Resource();
        resource.setId(id);
        resource.setActive(true);

        when(repository.findById(id)).thenReturn(Optional.of(resource));

        service.deleteResource(id);

        assertThat(resource.isActive()).isFalse();
        verify(repository).save(resource);
    }

    // =========================================
    // GET RESOURCES (pagination)
    // =========================================
    @Test
    void getResources_shouldReturnPagedResponse() {

        Page<Resource> page = new PageImpl<>(List.of(new Resource()));

        when(repository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Resource>>any(),
                any(Pageable.class)
        )).thenReturn(page);
        when(mapper.toDto(any())).thenReturn(
                new com.agora.dto.response.resource.ResourceDto(
                        UUID.randomUUID(), "Salle", ResourceType.IMMOBILIER,
                        100, "desc", 15000,
                        ResourceTestData.equipmentImage(),
                        List.of(), true
                )
        );

        var result = service.getResources(null, null, null, null, 0, 10);

        assertThat(result.content()).hasSize(1);
    }
}