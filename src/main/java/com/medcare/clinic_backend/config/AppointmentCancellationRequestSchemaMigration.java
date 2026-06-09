package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentCancellationRequestSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AppointmentCancellationRequestSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                create table if not exists public.appointment_cancellation_requests (
                    id serial primary key,
                    appointment_id integer not null,
                    patient_id integer not null,
                    invoice_id integer null,
                    cancel_reason text not null,
                    bank_name varchar(100),
                    bank_account_number varchar(50),
                    bank_account_holder varchar(100),
                    patient_note text,
                    refund_amount double precision default 0,
                    status varchar(50) not null default 'PENDING',
                    admin_note text,
                    processed_by_admin_id integer null,
                    processed_at timestamp null,
                    created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp
                )
                """);

        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists cancel_reason text");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists bank_name varchar(100)");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists bank_account_number varchar(50)");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists bank_account_holder varchar(100)");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists patient_note text");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists refund_amount double precision default 0");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists status varchar(50) default 'PENDING'");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists admin_note text");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists processed_by_admin_id integer");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists processed_at timestamp");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists created_at timestamp default current_timestamp");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists updated_at timestamp default current_timestamp");
        jdbcTemplate.execute("alter table if exists public.appointment_cancellation_requests add column if not exists invoice_id integer");

        jdbcTemplate.execute("""
                update public.appointment_cancellation_requests
                set created_at = coalesce(created_at, current_timestamp)
                where created_at is null
                """);
        jdbcTemplate.execute("""
                update public.appointment_cancellation_requests
                set updated_at = coalesce(updated_at, created_at, current_timestamp)
                where updated_at is null
                """);
        jdbcTemplate.execute("""
                update public.appointment_cancellation_requests
                set status = coalesce(nullif(btrim(status), ''), 'PENDING')
                where status is null or btrim(status) = ''
                """);

        jdbcTemplate.execute("""
                create index if not exists idx_cancel_request_status
                on public.appointment_cancellation_requests(status)
                """);

        jdbcTemplate.execute("""
                create index if not exists idx_cancel_request_appointment
                on public.appointment_cancellation_requests(appointment_id)
                """);

        jdbcTemplate.execute("""
                create index if not exists idx_cancel_request_patient
                on public.appointment_cancellation_requests(patient_id)
                """);

        jdbcTemplate.execute("""
                create index if not exists idx_cancel_request_created_at
                on public.appointment_cancellation_requests(created_at desc)
                """);

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from information_schema.table_constraints
                        where constraint_name = 'fk_cancel_request_appointment'
                          and table_name = 'appointment_cancellation_requests'
                          and table_schema = 'public'
                    ) then
                        alter table public.appointment_cancellation_requests
                        add constraint fk_cancel_request_appointment
                        foreign key (appointment_id)
                        references public.appointments(id)
                        on delete cascade;
                    end if;
                end $$;
                """);

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from information_schema.table_constraints
                        where constraint_name = 'fk_cancel_request_patient'
                          and table_name = 'appointment_cancellation_requests'
                          and table_schema = 'public'
                    ) then
                        alter table public.appointment_cancellation_requests
                        add constraint fk_cancel_request_patient
                        foreign key (patient_id)
                        references public.patients(id)
                        on delete cascade;
                    end if;
                end $$;
                """);

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from information_schema.table_constraints
                        where constraint_name = 'fk_cancel_request_invoice'
                          and table_name = 'appointment_cancellation_requests'
                          and table_schema = 'public'
                    ) then
                        alter table public.appointment_cancellation_requests
                        add constraint fk_cancel_request_invoice
                        foreign key (invoice_id)
                        references public.invoices(id)
                        on delete set null;
                    end if;
                end $$;
                """);

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from information_schema.table_constraints
                        where constraint_name = 'fk_cancel_request_admin'
                          and table_name = 'appointment_cancellation_requests'
                          and table_schema = 'public'
                    ) then
                        alter table public.appointment_cancellation_requests
                        add constraint fk_cancel_request_admin
                        foreign key (processed_by_admin_id)
                        references public.accounts(id)
                        on delete set null;
                    end if;
                end $$;
                """);
    }
}
