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
        jdbcTemplate.execute("alter table website_feedbacks add column if not exists status varchar(20)");
        jdbcTemplate.execute("update website_feedbacks set status = 'APPROVED' where is_approved = true and (status is null or trim(status) = '')");
        jdbcTemplate.execute("update website_feedbacks set status = 'PENDING' where status is null or trim(status) = ''");
        jdbcTemplate.execute("alter table website_feedbacks alter column status set default 'PENDING'");
    }
}
