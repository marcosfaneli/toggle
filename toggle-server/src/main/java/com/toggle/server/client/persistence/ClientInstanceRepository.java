package com.toggle.server.client.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface ClientInstanceRepository extends JpaRepository<ClientInstanceEntity, Long> {

    Optional<ClientInstanceEntity> findByServiceNameAndInstanceId(String serviceName, String instanceId);

    List<ClientInstanceEntity> findAllByOrderByServiceNameAscInstanceIdAsc();

    List<ClientInstanceEntity> findAllByServiceNameOrderByInstanceIdAsc(String serviceName);

    List<ClientInstanceEntity> findAllByStatusAndLastHeartbeatAtBefore(String status, Instant threshold);

    @Query("""
        SELECT ci FROM ClientInstanceEntity ci
        WHERE ci.id IN (
            SELECT cts.clientInstanceId FROM ClientSubscriptionEntity cts
            WHERE cts.toggleName = :toggleName
        )
        AND ci.status = :status
        """)
    List<ClientInstanceEntity> findActiveInstancesForToggle(
            @Param("toggleName") String toggleName,
            @Param("status") String status);
}
