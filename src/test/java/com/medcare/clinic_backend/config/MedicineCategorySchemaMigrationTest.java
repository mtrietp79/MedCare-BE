package com.medcare.clinic_backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicineCategorySchemaMigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    @Test
    void run_shouldExecuteIdempotentMedicineCategoryMigration() throws Exception {
        MedicineCategorySchemaMigration migration = new MedicineCategorySchemaMigration(jdbcTemplate);

        migration.run(mock(ApplicationArguments.class));

        verify(jdbcTemplate, atLeast(4)).execute(sqlCaptor.capture());
        List<String> sqlStatements = sqlCaptor.getAllValues();

        assertTrue(
                sqlStatements.stream().anyMatch(sql -> sql.contains("add column if not exists medicine_category")),
                "must add medicine_category column"
        );
        assertTrue(
                sqlStatements.stream().anyMatch(sql -> sql.contains("coalesce(nullif(btrim(medicine_category), ''), nullif(btrim(unit), ''), 'Kh\u00E1c')")),
                "must backfill medicine_category from existing values"
        );
        assertTrue(
                sqlStatements.stream().anyMatch(sql -> sql.contains("alter column medicine_category set not null")),
                "must enforce NOT NULL on medicine_category"
        );
        assertTrue(
                sqlStatements.stream().anyMatch(sql -> sql.contains("idx_medicines_category")),
                "must create index idx_medicines_category"
        );
    }
}
