package com.agora.service;



import com.agora.dto.request.ResourceRequest;
import com.agora.dto.response.PagedResponse;
import com.agora.dto.response.ResourceDto;
import com.agora.dto.response.TimeSlotDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResourceService {

    PagedResponse<ResourceDto> getResources(
            String type,
            Integer minCapacity,
            Boolean available,
            LocalDate date,
            int page,
            int size
    );

    ResourceDto getResourceById(UUID resourceId);

    List<TimeSlotDto> getSlots(Long resourceId, LocalDate date);

    List<TimeSlotDto> getSlots(UUID resourceId, LocalDate date);

    ResourceDto createResource(ResourceRequest request);

    ResourceDto updateResource(UUID resourceId, ResourceRequest request);

    void deleteResource(UUID resourceId);
}