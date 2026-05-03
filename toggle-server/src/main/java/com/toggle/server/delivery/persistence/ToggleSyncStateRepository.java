package com.toggle.server.delivery.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ToggleSyncStateRepository extends JpaRepository<ToggleSyncStateEntity, Long> {

    Optional<ToggleSyncStateEntity> findByToggleIdAndClientInstanceId(Long toggleId, Long clientInstanceId);
}
