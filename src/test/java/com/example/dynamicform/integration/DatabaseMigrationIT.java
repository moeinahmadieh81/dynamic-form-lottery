package com.example.dynamicform.integration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationIT extends IntegrationTestSupport {

    @Test
    void flywayShouldBuildTheExpectedSchemaFromScratch() {
        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true and version is not null",
                Integer.class
        );
        assertThat(successfulMigrations).isEqualTo(5);

        List<String> tables = jdbcTemplate.queryForList(
                """
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                """,
                String.class
        );

        assertThat(tables).contains(
                "users",
                "forms",
                "form_versions",
                "submissions",
                "lotteries",
                "lottery_entries",
                "lottery_winners",
                "audit_logs",
                "flyway_schema_history"
        );
    }
}
