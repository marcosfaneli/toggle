package com.toggle.server.toggle.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
class TogglePaginationLinkBuilder {

    List<String> buildLinks(HttpServletRequest request, String ownerServiceName, Boolean enabled,
                            Slice<?> slice, Pageable pageable) {
        var links = new ArrayList<String>();
        links.add(link(pageUri(request, ownerServiceName, enabled, pageable.getPageSize(), 0), "first"));
        if (!slice.isFirst()) {
            links.add(link(pageUri(request, ownerServiceName, enabled, pageable.getPageSize(), pageable.getPageNumber() - 1), "prev"));
        }
        if (slice.hasNext()) {
            links.add(link(pageUri(request, ownerServiceName, enabled, pageable.getPageSize(), pageable.getPageNumber() + 1), "next"));
        }
        return links;
    }

    private String pageUri(HttpServletRequest request, String ownerServiceName, Boolean enabled, int size, int page) {
        var uri = Objects.requireNonNull(request.getRequestURL().toString());
        var builder = UriComponentsBuilder.fromUriString(uri)
                .queryParam("size", size)
                .queryParam("page", page);
        if (ownerServiceName != null && !ownerServiceName.isBlank()) {
            builder.queryParam("ownerServiceName", ownerServiceName);
        }
        if (enabled != null) {
            builder.queryParam("enabled", enabled);
        }
        return builder.build().toUriString();
    }

    private String link(String uri, String rel) {
        return "<%s>; rel=\"%s\"".formatted(uri, rel);
    }
}
