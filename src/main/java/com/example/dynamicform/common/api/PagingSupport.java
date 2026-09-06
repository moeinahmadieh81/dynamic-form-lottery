package com.example.dynamicform.common.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PagingSupport {

    private static final int MAX_PAGE_SIZE = 100;

    private PagingSupport() {
    }

    public static Pageable pageRequest(int page,
                                       int size,
                                       String sortBy,
                                       String direction,
                                       Set<String> allowedSortFields,
                                       String defaultSort) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        String resolvedSort = (sortBy == null || sortBy.isBlank()) ? defaultSort : sortBy;
        if (!allowedSortFields.contains(resolvedSort)) {
            throw new IllegalArgumentException("Unsupported sort field: " + resolvedSort);
        }

        Sort.Direction resolvedDirection;
        try {
            resolvedDirection = Sort.Direction.fromString(direction == null ? "desc" : direction);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("direction must be 'asc' or 'desc'");
        }

        return PageRequest.of(page, size, Sort.by(resolvedDirection, resolvedSort));
    }
}
