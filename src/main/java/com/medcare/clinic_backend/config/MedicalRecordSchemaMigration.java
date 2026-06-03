package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MedicalRecordSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public MedicalRecordSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("alter table if exists medical_records add column if not exists follow_up_appointment_id integer");
        jdbcTemplate.execute("alter table if exists medical_records add column if not exists medical_record_code varchar(30)");
        jdbcTemplate.execute("alter table if exists medical_records add column if not exists created_at timestamp without time zone");

        jdbcTemplate.execute("""
                update medical_records
                set created_at = coalesce(created_at, now())
                where created_at is null
                """);

        jdbcTemplate.execute("""
                update medical_records
                set medical_record_code = 'BA-' || lpad(id::text, 8, '0')
                where medical_record_code is null or btrim(medical_record_code) = ''
                """);

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from information_schema.table_constraints
                        where table_schema = 'public'
                          and table_name = 'medical_records'
                          and constraint_name = 'uk_medical_record_code'
                    ) then
                        alter table public.medical_records
                        add constraint uk_medical_record_code unique (medical_record_code);
                    end if;
                end $$;
                """);
    }
}
