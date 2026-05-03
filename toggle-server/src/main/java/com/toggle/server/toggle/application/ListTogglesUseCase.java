package com.toggle.server.toggle.application;

import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class ListTogglesUseCase {

    private final TogglePersistenceAdapter persistenceAdapter;

    public ListTogglesUseCase(TogglePersistenceAdapter persistenceAdapter) {
        this.persistenceAdapter = persistenceAdapter;
    }

    public Slice<Toggle> execute(ListTogglesQuery query, Pageable pageable) {
        return persistenceAdapter.findAll(query.ownerServiceName(), query.enabled(), pageable);
    }
}
