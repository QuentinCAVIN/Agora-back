package com.agora.service.resource;

import com.agora.dto.request.resource.ResourceRequest;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.resource.ResourceDto;
import com.agora.dto.response.resource.TimeSlotDto;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResourceService {

    PagedResponse<ResourceDto> getResources(
            Authentication authentication,
            String type,
            Integer minCapacity,
            Boolean available,
            LocalDate date,
            int page,
            int size
    );

    ResourceDto getResourceById(UUID resourceId);

    List<TimeSlotDto> getSlots(UUID resourceId, LocalDate date);

    ResourceDto createResource(ResourceRequest request);

    ResourceDto updateResource(UUID resourceId, ResourceRequest request);

    void deleteResource(UUID resourceId);
}
