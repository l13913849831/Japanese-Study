package com.jp.vocab.shared.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements()
        );
    }

    public static <T> PageResponse<T> empty(int page, int pageSize) {
        return new PageResponse<>(List.of(), page, pageSize, 0);
    }
}
