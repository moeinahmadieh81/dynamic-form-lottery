package com.example.dynamicform.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagingSupportTest {

    @Test
    void createsWhitelistedPageRequest() {
        Pageable pageable = PagingSupport.pageRequest(
                1,
                25,
                "submittedAt",
                "asc",
                Set.of("submittedAt", "id"),
                "id"
        );

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("submittedAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("submittedAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void rejectsUnsupportedSortProperty() {
        assertThatThrownBy(() -> PagingSupport.pageRequest(
                0,
                20,
                "passwordHash",
                "desc",
                Set.of("submittedAt", "id"),
                "id"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort field");
    }

    @Test
    void rejectsOversizedPage() {
        assertThatThrownBy(() -> PagingSupport.pageRequest(
                0,
                101,
                "id",
                "desc",
                Set.of("id"),
                "id"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 100");
    }
}
