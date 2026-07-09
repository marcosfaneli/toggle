package com.toggle.server.serviceauth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ServiceApiKeyRepository extends JpaRepository<ServiceApiKeyEntity, Long> {

    Optional<ServiceApiKeyEntity> findByKeyHashAndStatus(String keyHash, String status);

    Optional<ServiceApiKeyEntity> findByPublicId(String publicId);

    List<ServiceApiKeyEntity> findAllByOrderByCreatedAtDesc();
}
