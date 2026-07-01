package com.toggle.server.toggle.web;

import com.toggle.server.client.application.ToggleConsumerView;
import org.springframework.data.domain.Slice;

import java.util.List;

public record PagedToggleConsumerResponse(
        List<ToggleConsumerResponse> content,
        int number,
        int size,
        boolean first,
        boolean last) {

    public static PagedToggleConsumerResponse from(Slice<ToggleConsumerView> slice) {
        return new PagedToggleConsumerResponse(
                slice.getContent().stream().map(ToggleConsumerResponse::from).toList(),
                slice.getNumber(),
                slice.getSize(),
                slice.isFirst(),
                slice.isLast());
    }
}
