package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WebsiteFeedbackStatusColumnMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public WebsiteFeedbackStatusColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                do $$
                begin
                    if exists (
                        select 1
                        from information_schema.tables
                        where table_schema = 'public'
                          and table_name = 'website_feedbacks'
                    ) then
                        alter table public.website_feedbacks add column if not exists full_name varchar(100);
                        alter table public.website_feedbacks add column if not exists email varchar(100);
                        alter table public.website_feedbacks add column if not exists is_approved boolean default false;
                        alter table public.website_feedbacks add column if not exists created_at timestamp without time zone;
                        alter table public.website_feedbacks add column if not exists status varchar(20);
                    end if;
                end $$;
                """);

        jdbcTemplate.execute("""
                update public.website_feedbacks
                set created_at = coalesce(created_at, now())
                where created_at is null
                """);

        jdbcTemplate.execute("""
                update public.website_feedbacks
                set status = 'APPROVED'
                where coalesce(is_approved, false) = true
                  and (status is null or trim(status) = '')
                """);

        jdbcTemplate.execute("""
                update public.website_feedbacks
                set status = 'PENDING'
                where status is null or trim(status) = ''
                """);

        jdbcTemplate.execute("""
                alter table public.website_feedbacks
                alter column status set default 'PENDING'
                """);
    }
}
