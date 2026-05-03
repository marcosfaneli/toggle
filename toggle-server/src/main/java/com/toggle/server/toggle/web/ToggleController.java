package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.application.CreateToggleUseCase;
import com.toggle.server.toggle.application.ListTogglesQuery;
import com.toggle.server.toggle.application.ListTogglesUseCase;
import com.toggle.server.toggle.application.UpdateToggleCommand;
import com.toggle.server.toggle.application.UpdateToggleUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;

import java.util.ArrayList;

@RestController
@RequestMapping("/toggles")
public class ToggleController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CreateToggleUseCase createToggleUseCase;
    private final ListTogglesUseCase listTogglesUseCase;
    private final UpdateToggleUseCase updateToggleUseCase;

    public ToggleController(CreateToggleUseCase createToggleUseCase, ListTogglesUseCase listTogglesUseCase, UpdateToggleUseCase updateToggleUseCase) {
        this.createToggleUseCase = createToggleUseCase;
        this.listTogglesUseCase = listTogglesUseCase;
        this.updateToggleUseCase = updateToggleUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ToggleResponse create(@Valid @RequestBody CreateToggleRequest request) {
        CreateToggleCommand.ToggleValueCommand valueCommand = null;
        if (request.value() != null) {
            valueCommand = new CreateToggleCommand.ToggleValueCommand(
                    request.value().type(),
                    request.value().raw());
        }

        var command = new CreateToggleCommand(
                request.name(),
                request.ownerServiceName(),
                request.enabled(),
                valueCommand);

        return ToggleResponse.from(createToggleUseCase.execute(command));
    }

    @GetMapping
    public ResponseEntity<PagedToggleResponse> list(
            @RequestParam String ownerServiceName,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be <= " + MAX_PAGE_SIZE);
        }

        var query = new ListTogglesQuery(ownerServiceName, enabled);
        var slice = listTogglesUseCase.execute(query, pageable);
        var body = PagedToggleResponse.from(slice);

        var links = new ArrayList<String>();
        links.add(buildLink(pageUri(request, ownerServiceName, enabled, pageable.getPageSize(), 0), "first"));
        if (!slice.isFirst()) {
            links.add(buildLink(pageUri(request, ownerServiceName, enabled, pageable.getPageSize(), pageable.getPageNumber() - 1), "prev"));
        }
        if (slice.hasNext()) {
            links.add(buildLink(pageUri(request, ownerServiceName, enabled, pageable.getPageSize(), pageable.getPageNumber() + 1), "next"));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.join(", ", links))
                .body(body);
    }

    private String pageUri(HttpServletRequest request, String ownerServiceName, Boolean enabled, int size, int page) {
        var builder = UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
                .queryParam("ownerServiceName", ownerServiceName)
                .queryParam("size", size)
                .queryParam("page", page);
        if (enabled != null) {
            builder.queryParam("enabled", enabled);
        }
        return builder.build().toUriString();
    }

    private String buildLink(String uri, String rel) {
        return "<%s>; rel=\"%s\"".formatted(uri, rel);
    }

    @PatchMapping("/{name}")
    public ResponseEntity<ToggleResponse> update(
            @PathVariable String name,
            @RequestParam String ownerServiceName,
            @Valid @RequestBody UpdateToggleRequest request) {

        UpdateToggleCommand.ValueUpdate valueUpdate;
        if (!request.isValueExplicitlySet()) {
            valueUpdate = new UpdateToggleCommand.ValueUpdate.Keep();
        } else if (request.getValue() == null) {
            valueUpdate = new UpdateToggleCommand.ValueUpdate.Remove();
        } else {
            valueUpdate = new UpdateToggleCommand.ValueUpdate.Set(
                    request.getValue().type(),
                    request.getValue().raw());
        }

        var command = new UpdateToggleCommand(name, ownerServiceName, request.getEnabled(), valueUpdate);
        return ResponseEntity.ok(ToggleResponse.from(updateToggleUseCase.execute(command)));
    }
}
