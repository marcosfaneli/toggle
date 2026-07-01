package com.toggle.server.toggle.web;

import com.toggle.server.toggle.domain.Toggle;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TogglePaginationLinkBuilderTest {

    private final TogglePaginationLinkBuilder builder = new TogglePaginationLinkBuilder();

    private static Toggle anyToggle() {
        return new Toggle("01ID", "t", "svc", true, 1L, Instant.now(), null);
    }

    @SuppressWarnings("null")
    private static Slice<Toggle> slice(int page, boolean hasNext) {
        return new SliceImpl<>(List.of(anyToggle()), PageRequest.of(page, 20), hasNext);
    }

    private MockHttpServletRequest mockRequest() {
        var req = new MockHttpServletRequest("GET", "/toggles");
        req.setServerName("localhost");
        req.setServerPort(8080);
        return req;
    }

    @Test
    void shouldAlwaysIncludeFirstLink() {
        var pageable = PageRequest.of(0, 20);

        var links = builder.buildLinks(mockRequest(), "svc", null, slice(0, false), pageable);

        assertThat(links).anyMatch(l -> l.contains("rel=\"first\""));
    }

    @Test
    void shouldIncludeNextLinkWhenHasNext() {
        var pageable = PageRequest.of(0, 20);

        var links = builder.buildLinks(mockRequest(), "svc", null, slice(0, true), pageable);

        assertThat(links)
                .anyMatch(l -> l.contains("rel=\"next\""))
                .noneMatch(l -> l.contains("rel=\"prev\""));
    }

    @Test
    void shouldIncludePrevLinkOnSecondPage() {
        var pageable = PageRequest.of(1, 20);

        var links = builder.buildLinks(mockRequest(), "svc", null, slice(1, false), pageable);

        assertThat(links)
                .anyMatch(l -> l.contains("rel=\"prev\""))
                .noneMatch(l -> l.contains("rel=\"next\""));
    }

    @Test
    void shouldIncludeEnabledParamWhenProvided() {
        var pageable = PageRequest.of(0, 20);

        var links = builder.buildLinks(mockRequest(), "svc", true, slice(0, true), pageable);

        assertThat(links).anyMatch(l -> l.contains("enabled=true"));
    }

    @Test
    void shouldOmitEnabledParamWhenNull() {
        var pageable = PageRequest.of(0, 20);

        var links = builder.buildLinks(mockRequest(), "svc", null, slice(0, false), pageable);

        assertThat(links).noneMatch(l -> l.contains("enabled="));
    }

    @Test
    void shouldOmitMaintainerWhenNull() {
        var pageable = PageRequest.of(0, 20);

        var links = builder.buildLinks(mockRequest(), null, null, slice(0, false), pageable);

        assertThat(links).noneMatch(l -> l.contains("maintainer="));
    }
}
