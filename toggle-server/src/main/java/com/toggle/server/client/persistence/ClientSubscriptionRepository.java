package com.toggle.server.client.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClientSubscriptionRepository extends JpaRepository<ClientSubscriptionEntity, Long> {

    void deleteAllByClientInstanceId(Long clientInstanceId);
}
