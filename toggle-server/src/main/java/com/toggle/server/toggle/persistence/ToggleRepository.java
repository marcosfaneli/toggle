package com.toggle.server.toggle.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ToggleRepository extends JpaRepository<ToggleEntity, Long> {
}
