package com.toggle.server.toggle.web;

import com.toggle.server.toggle.domain.Toggle;
import org.springframework.data.domain.Slice;

import java.util.List;

public record PagedToggleResponse(
        List<ToggleResponse> content,
        int number,
        int size,
        boolean first,
        boolean last) {

    public static PagedToggleResponse from(Slice<Toggle> slice) {
        return new PagedToggleResponse(
                slice.getContent().stream().map(ToggleResponse::from).toList(),
                slice.getNumber(),
                slice.getSize(),
                slice.isFirst(),
                slice.isLast());
    }
}
