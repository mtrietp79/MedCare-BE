package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentFollowUpSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AppointmentFollowUpSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateAppointments();
        migrateMedicalRecords();
        migrateInvoices();
    }

    private void migrateAppointments() {
        jdbcTemplate.execute("""
                alter table if exists public.appointments
                add column if not exists appointment_type varchar(50) default 'Khám bệnh',
                add column if not exists parent_appointment_id integer null,
                add column if not exists follow_up_note text,
                add column if not exists consultation_fee double precision default 0,
                add column if not exists payment_status varchar(50) default 'UNPAID'
                """);

        jdbcTemplate.execute("""
                update public.appointments
                set appointment_type = 'Khám bệnh'
                where appointment_type is null or btrim(appointment_type) = ''
                """);

        jdbcTemplate.execute("""
                update public.appointments
                set payment_status = 'UNPAID'
                where payment_status is null or btrim(payment_status) = ''
                """);

        jdbcTemplate.execute("""
                update public.appointments
                set status = 'CONFIRMED'
                where upper(coalesce(payment_status, '')) in ('PAID', 'PAID_ONLINE')
                  and upper(coalesce(status, '')) in ('PENDING', 'PENDING_PAYMENT')
                """);

        jdbcTemplate.execute("""
                update public.appointments a
                set consultation_fee = case
                        when lower(coalesce(a.appointment_type, '')) like '%tái%'
                             or lower(coalesce(a.appointment_type, '')) like '%tai%'
                            then d.price * 0.5
                        else d.price
                    end
                from public.doctors d
                where a.doctor_id = d.id
                  and d.price is not null
                  and (a.consultation_fee is null or a.consultation_fee <= 0)
                """);

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from information_schema.table_constraints
                        where constraint_name = 'fk_appointments_parent'
                          and table_name = 'appointments'
                          and table_schema = 'public'
                    ) then
                        alter table public.appointments
                        add constraint fk_appointments_parent
                        foreign key (parent_appointment_id)
                        references public.appointments(id)
                        on delete set null;
                    end if;
                end $$;
                """);
    }

    private void migrateMedicalRecords() {
        jdbcTemplate.execute("""
                alter table if exists public.medical_records
                add column if not exists type varchar(50) default 'Khám bệnh'
                """);

        jdbcTemplate.execute("""
                update public.medical_records mr
                set type = coalesce(nullif(btrim(a.appointment_type), ''), 'Khám bệnh')
                from public.appointments a
                where mr.appointment_id = a.id
                  and (mr.type is null or btrim(mr.type) = '')
                """);
    }

    private void migrateInvoices() {
        jdbcTemplate.execute("""
                alter table if exists public.invoices
                add column if not exists appointment_id integer null,
                add column if not exists consultation_fee double precision default 0
                """);

        jdbcTemplate.execute("""
                update public.invoices i
                set appointment_id = mr.appointment_id
                from public.medical_records mr
                where i.medical_record_id = mr.id
                  and i.appointment_id is null
                """);

        jdbcTemplate.execute("""
                update public.invoices i
                set consultation_fee = coalesce(a.consultation_fee, 0)
                from public.appointments a
                where i.appointment_id = a.id
                  and (i.consultation_fee is null or i.consultation_fee <= 0)
                """);

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from information_schema.table_constraints
                        where constraint_name = 'fk_invoice_appointment'
                          and table_name = 'invoices'
                          and table_schema = 'public'
                    ) then
                        alter table public.invoices
                        add constraint fk_invoice_appointment
                        foreign key (appointment_id)
                        references public.appointments(id)
                        on delete set null;
                    end if;
                end $$;
                """);
    }
}
