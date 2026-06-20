package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.CreateToggleUseCase;
import com.toggle.server.toggle.application.GetToggleByNameUseCase;
import com.toggle.server.toggle.application.ListTogglesQuery;
import com.toggle.server.toggle.application.ListTogglesUseCase;
import com.toggle.server.toggle.application.UpdateToggleUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/toggles")
public class ToggleController {

    private static final Logger log = LoggerFactory.getLogger(ToggleController.class);
    private static final String SORT_PROPERTY_UPDATED_AT = "updatedAt";
    private static final String SORT_ALIAS_CREATED_AT = "createdAt";

    private final CreateToggleUseCase createToggleUseCase;
    private final GetToggleByNameUseCase getToggleByNameUseCase;
    private final ListTogglesUseCase listTogglesUseCase;
    private final UpdateToggleUseCase updateToggleUseCase;
    private final CreateToggleCommandMapper createMapper;
    private final UpdateToggleCommandMapper updateMapper;
    private final TogglePaginationLinkBuilder linkBuilder;

    @Value("${toggle.api.max-page-size:100}")
    private int maxPageSize;

    public ToggleController(CreateToggleUseCase createToggleUseCase,
                            GetToggleByNameUseCase getToggleByNameUseCase,
                            ListTogglesUseCase listTogglesUseCase,
                            UpdateToggleUseCase updateToggleUseCase,
                            CreateToggleCommandMapper createMapper,
                            UpdateToggleCommandMapper updateMapper,
                            TogglePaginationLinkBuilder linkBuilder) {
        this.createToggleUseCase = createToggleUseCase;
        this.getToggleByNameUseCase = getToggleByNameUseCase;
        this.listTogglesUseCase = listTogglesUseCase;
        this.updateToggleUseCase = updateToggleUseCase;
        this.createMapper = createMapper;
        this.updateMapper = updateMapper;
        this.linkBuilder = linkBuilder;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ToggleResponse create(@Valid @RequestBody CreateToggleRequest request) {
        return ToggleResponse.from(createToggleUseCase.execute(createMapper.toCommand(request)));
    }

    @GetMapping("/{name}")
    public ToggleResponse getByName(@PathVariable String name) {
        return ToggleResponse.from(getToggleByNameUseCase.execute(name));
    }

    @GetMapping
    public ResponseEntity<PagedToggleResponse> list(
            @RequestParam(required = false) String ownerServiceName,
            @RequestParam(required = false) Boolean enabled,
            @ParameterObject
            @PageableDefault(size = 20, sort = SORT_PROPERTY_UPDATED_AT, direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        var normalizedPageable = normalizeSortAliases(pageable);

        if (normalizedPageable.getPageSize() > maxPageSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be <= " + maxPageSize);
        }

        var slice = listTogglesUseCase.execute(new ListTogglesQuery(ownerServiceName, enabled), normalizedPageable);
        var links = linkBuilder.buildLinks(request, ownerServiceName, enabled, slice, normalizedPageable);

        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.join(", ", links))
                .body(PagedToggleResponse.from(slice));
    }

    private Pageable normalizeSortAliases(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        var mappedOrders = pageable.getSort().stream()
                .map(order -> SORT_ALIAS_CREATED_AT.equals(order.getProperty())
                        ? new Sort.Order(order.getDirection(), SORT_PROPERTY_UPDATED_AT)
                        : order)
                .toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(mappedOrders));
    }

    @PatchMapping("/{name}")
    public ResponseEntity<ToggleResponse> update(
            @PathVariable String name,
            @Valid @RequestBody UpdateToggleRequest request) {
        log.info(
                "event=toggle_update_requested name={} enabled={} hasValue={}",
                name,
                request.getEnabled(),
                request.getValue() != null);
        return ResponseEntity.ok(
                ToggleResponse.from(updateToggleUseCase.execute(
                updateMapper.toCommand(name, request))));
    }
}
