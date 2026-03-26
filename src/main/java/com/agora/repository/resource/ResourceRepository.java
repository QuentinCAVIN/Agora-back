package com.agora.repository.resource;

import com.agora.entity.resource.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID>, JpaSpecificationExecutor<Resource> {
    Page<Resource> findByActiveTrue(Pageable pageable);

    Optional<Resource> findByNameIgnoreCase(String name);
}
