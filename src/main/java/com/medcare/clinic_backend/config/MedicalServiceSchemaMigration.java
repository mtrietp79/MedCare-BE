package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MedicalServiceSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public MedicalServiceSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("alter table medical_services add column if not exists active boolean");
        jdbcTemplate.execute("update medical_services set active = true where active is null");
        jdbcTemplate.execute("alter table medical_services alter column active set default true");

        jdbcTemplate.execute("alter table medical_services add column if not exists advertised boolean");
        jdbcTemplate.execute("update medical_services set advertised = false where advertised is null");
        jdbcTemplate.execute("alter table medical_services alter column advertised set default false");

        jdbcTemplate.execute("alter table medical_services add column if not exists specialty_id integer");
        jdbcTemplate.execute(
                """
                update medical_services ms
                set specialty_id = d.specialty_id
                from doctors d
                where ms.specialty_id is null
                  and ms.assigned_doctor_id = d.id
                  and d.specialty_id is not null
                """
        );
        jdbcTemplate.execute(
                """
                update medical_services
                set specialty_id = (
                    select s.id
                    from specialties s
                    order by s.id
                    limit 1
                )
                where specialty_id is null
                  and exists (select 1 from specialties)
                """
        );
    }
}
