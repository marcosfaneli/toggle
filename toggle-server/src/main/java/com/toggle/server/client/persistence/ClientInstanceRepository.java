package com.toggle.server.client.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface ClientInstanceRepository extends JpaRepository<ClientInstanceEntity, Long> {

    Optional<ClientInstanceEntity> findByServiceNameAndInstanceId(String serviceName, String instanceId);

    List<ClientInstanceEntity> findAllByStatusAndLastHeartbeatAtBefore(String status, Instant threshold);
}
