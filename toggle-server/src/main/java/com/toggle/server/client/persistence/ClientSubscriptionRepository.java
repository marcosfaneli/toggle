package com.toggle.server.client.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ClientSubscriptionRepository extends JpaRepository<ClientSubscriptionEntity, Long> {

    void deleteAllByClientInstanceId(Long clientInstanceId);

    List<ClientSubscriptionEntity> findByToggleName(String toggleName);

    List<ClientSubscriptionEntity> findAllByClientInstanceIdInOrderByToggleNameAsc(List<Long> clientInstanceIds);

    @Query("""
        select
            ci.serviceName as serviceName,
            ci.instanceId as instanceId,
            ci.podName as podName,
            ci.namespace as namespace,
            ci.callbackUrl as callbackUrl,
            ci.status as status,
            cts.consumeMode as consumeMode
        from ClientSubscriptionEntity cts
        join ClientInstanceEntity ci on ci.id = cts.clientInstanceId
        where cts.toggleName = :toggleName
        order by ci.serviceName asc, ci.instanceId asc
        """)
    Slice<ToggleConsumerRow> findConsumersByToggleName(
            @Param("toggleName") String toggleName,
            Pageable pageable);

    interface ToggleConsumerRow {
        String getServiceName();
        String getInstanceId();
        String getPodName();
        String getNamespace();
        String getCallbackUrl();
        String getStatus();
        String getConsumeMode();
    }
}
