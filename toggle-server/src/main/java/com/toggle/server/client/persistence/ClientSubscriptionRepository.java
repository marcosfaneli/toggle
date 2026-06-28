package com.toggle.server.client.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ClientSubscriptionRepository extends JpaRepository<ClientSubscriptionEntity, Long> {

    void deleteAllByClientInstanceId(Long clientInstanceId);

    List<ClientSubscriptionEntity> findByToggleName(String toggleName);

    List<ClientSubscriptionEntity> findAllByClientInstanceIdInOrderByToggleNameAsc(List<Long> clientInstanceIds);
}
