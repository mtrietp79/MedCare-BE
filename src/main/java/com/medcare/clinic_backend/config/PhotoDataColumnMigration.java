package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PhotoDataColumnMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public PhotoDataColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateOidPhotoColumnToBytea("doctor_photos");
        migrateOidPhotoColumnToBytea("medical_service_photos");
    }

    private void migrateOidPhotoColumnToBytea(String tableName) {
        String dataType = jdbcTemplate.queryForObject(
                """
                select data_type
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = ?
                  and column_name = 'data'
                """,
                String.class,
                tableName
        );

        if (!"oid".equalsIgnoreCase(dataType)) {
            return;
        }

        jdbcTemplate.execute("alter table " + tableName + " add column if not exists data_bytea bytea");
        jdbcTemplate.execute(
                "update " + tableName + " set data_bytea = " +
                        "case when exists (select 1 from pg_largeobject_metadata where oid = data) " +
                        "then lo_get(data) else decode('', 'hex') end " +
                        "where data is not null and data_bytea is null"
        );
        jdbcTemplate.execute("alter table " + tableName + " alter column data_bytea set not null");
        jdbcTemplate.execute("alter table " + tableName + " drop column data");
        jdbcTemplate.execute("alter table " + tableName + " rename column data_bytea to data");
    }
}
