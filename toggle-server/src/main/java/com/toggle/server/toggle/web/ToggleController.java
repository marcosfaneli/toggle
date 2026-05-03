package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.CreateToggleUseCase;
import com.toggle.server.toggle.application.ListTogglesQuery;
import com.toggle.server.toggle.application.ListTogglesUseCase;
import com.toggle.server.toggle.application.UpdateToggleUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/toggles")
public class ToggleController {

    private final CreateToggleUseCase createToggleUseCase;
    private final ListTogglesUseCase listTogglesUseCase;
    private final UpdateToggleUseCase updateToggleUseCase;
    private final CreateToggleCommandMapper createMapper;
    private final UpdateToggleCommandMapper updateMapper;
    private final TogglePaginationLinkBuilder linkBuilder;

    @Value("${toggle.api.max-page-size:100}")
    private int maxPageSize;

    public ToggleController(CreateToggleUseCase createToggleUseCase,
                            ListTogglesUseCase listTogglesUseCase,
                            UpdateToggleUseCase updateToggleUseCase,
                            CreateToggleCommandMapper createMapper,
                            UpdateToggleCommandMapper updateMapper,
                            TogglePaginationLinkBuilder linkBuilder) {
        this.createToggleUseCase = createToggleUseCase;
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

    @GetMapping
    public ResponseEntity<PagedToggleResponse> list(
            @RequestParam String ownerServiceName,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        if (pageable.getPageSize() > maxPageSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be <= " + maxPageSize);
        }

        var slice = listTogglesUseCase.execute(new ListTogglesQuery(ownerServiceName, enabled), pageable);
        var links = linkBuilder.buildLinks(request, ownerServiceName, enabled, slice, pageable);

        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.join(", ", links))
                .body(PagedToggleResponse.from(slice));
    }

    @PatchMapping("/{name}")
    public ResponseEntity<ToggleResponse> update(
            @PathVariable String name,
            @RequestParam String ownerServiceName,
            @Valid @RequestBody UpdateToggleRequest request) {

        return ResponseEntity.ok(
                ToggleResponse.from(updateToggleUseCase.execute(
                        updateMapper.toCommand(name, ownerServiceName, request))));
    }
}
