package com.toggle.server.toggle.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ToggleRepository extends JpaRepository<ToggleEntity, Long> {

    @EntityGraph(attributePaths = "value")
    Slice<ToggleEntity> findByOwnerServiceName(String ownerServiceName, Pageable pageable);

    @EntityGraph(attributePaths = "value")
    Slice<ToggleEntity> findByOwnerServiceNameAndEnabled(String ownerServiceName, boolean enabled, Pageable pageable);
}
