package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountPasswordSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AccountPasswordSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                alter table if exists public.accounts
                add column if not exists reset_otp varchar(255),
                add column if not exists otp_expiry_time timestamp,
                add column if not exists reset_token varchar(255),
                add column if not exists reset_token_expiry_time timestamp,
                add column if not exists must_change_password boolean default false,
                add column if not exists email_verified boolean default false,
                add column if not exists is_test_account boolean default false,
                add column if not exists otp_last_sent_at timestamp,
                add column if not exists otp_failed_attempts integer default 0
                """);

        jdbcTemplate.execute("""
                update public.accounts
                set must_change_password = false
                where must_change_password is null
                """);

        jdbcTemplate.execute("""
                update public.accounts
                set email_verified = false
                where email_verified is null
                """);

        jdbcTemplate.execute("""
                update public.accounts
                set is_test_account = false
                where is_test_account is null
                """);

        jdbcTemplate.execute("""
                update public.accounts
                set otp_failed_attempts = 0
                where otp_failed_attempts is null
                """);
    }
}
