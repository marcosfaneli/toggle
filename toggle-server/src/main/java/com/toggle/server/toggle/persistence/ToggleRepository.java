package com.toggle.server.toggle.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ToggleRepository extends JpaRepository<ToggleEntity, Long> {

    @EntityGraph(attributePaths = "value")
    List<ToggleEntity> findAllByNameIn(List<String> names);

    @EntityGraph(attributePaths = "value")
    Slice<ToggleEntity> findByOwnerServiceName(String ownerServiceName, Pageable pageable);

    @EntityGraph(attributePaths = "value")
    Slice<ToggleEntity> findByOwnerServiceNameAndEnabled(String ownerServiceName, boolean enabled, Pageable pageable);

    @EntityGraph(attributePaths = "value")
    Optional<ToggleEntity> findByName(String name);
}
