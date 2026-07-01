package com.toggle.server.toggle.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface ToggleRepository extends JpaRepository<ToggleEntity, Long> {

    @EntityGraph(attributePaths = "value")
    List<ToggleEntity> findAllByNameIn(List<String> names);

    @EntityGraph(attributePaths = "value")
    Slice<ToggleEntity> findByMaintainer(String maintainer, Pageable pageable);

    @EntityGraph(attributePaths = "value")
    Slice<ToggleEntity> findByMaintainerAndEnabled(String maintainer, boolean enabled, Pageable pageable);

    @EntityGraph(attributePaths = "value")
    Slice<ToggleEntity> findByEnabled(boolean enabled, Pageable pageable);

    @EntityGraph(attributePaths = "value")
    Optional<ToggleEntity> findByName(String name);

    @Query("select distinct t.maintainer from ToggleEntity t order by t.maintainer asc")
    List<String> findDistinctMaintainers();
}
