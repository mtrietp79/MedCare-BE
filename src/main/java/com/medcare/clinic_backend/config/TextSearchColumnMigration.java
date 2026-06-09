package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TextSearchColumnMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public TextSearchColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureTextColumn("patients", "full_name", "varchar(100)");
        ensureTextColumn("patients", "email", "varchar(100)");
        ensureTextColumn("patients", "phone", "varchar(15)");
        ensureTextColumn("patients", "address", "text");

        ensureTextColumn("contact_messages", "full_name", "varchar(100)");
        ensureTextColumn("contact_messages", "email", "varchar(100)");
        ensureTextColumn("contact_messages", "phone", "varchar(20)");
        ensureTextColumn("contact_messages", "subject", "varchar(255)");
        ensureTextColumn("contact_messages", "message", "text");
        ensureTextColumn("contact_messages", "admin_reply", "text");
        ensureTextColumn("contact_messages", "admin_note", "text");
    }

    private void ensureTextColumn(String tableName, String columnName, String targetType) {
        Boolean isBytea = jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = ?
                      and column_name = ?
                      and udt_name = 'bytea'
                )
                """, Boolean.class, tableName, columnName);
        if (!Boolean.TRUE.equals(isBytea)) {
            return;
        }
        jdbcTemplate.execute(String.format(
                "alter table public.%s alter column %s type %s using convert_from(%s, 'UTF8')",
                tableName,
                columnName,
                targetType,
                columnName
        ));
    }
}
