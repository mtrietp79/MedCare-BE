package com.medcare.clinic_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class MedicineCategorySchemaMigration implements ApplicationRunner {

    private static final String DEFAULT_CATEGORY_NAME = "Khác";
    private static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
            "Giảm đau - hạ sốt",
            "Kháng sinh",
            "Dị ứng",
            "Dạ dày - tiêu hóa",
            "Vitamin - khoáng chất",
            "Hô hấp - long đờm",
            "Thuốc bôi ngứa - dị ứng",
            "Thuốc bôi nấm da",
            "Nhỏ mắt - nhỏ mũi",
            "Vật tư y tế",
            DEFAULT_CATEGORY_NAME
    );

    private final JdbcTemplate jdbcTemplate;

    public MedicineCategorySchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateMedicineCategory();
    }

    private void migrateMedicineCategory() {
        migrateMedicineTextColumnsFromByteaIfNeeded();
        ensureMedicineCategoriesTable();
        ensureLegacyMedicineColumns();
        ensureMedicineCategoryForeignKeyColumn();
        seedDefaultCategories();
        migrateLegacyCategoryData();
        ensureMedicineCategoryForeignKey();
        ensureIndexes();
    }

    private void ensureMedicineCategoriesTable() {
        jdbcTemplate.execute("""
                create table if not exists public.medicine_categories (
                    id serial primary key,
                    name varchar(100) not null unique,
                    description text,
                    is_active boolean default true,
                    created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp
                )
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicine_categories
                add column if not exists description text
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicine_categories
                add column if not exists is_active boolean default true
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicine_categories
                add column if not exists created_at timestamp default current_timestamp
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicine_categories
                add column if not exists updated_at timestamp default current_timestamp
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicine_categories
                alter column is_active set default true
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicine_categories
                alter column created_at set default current_timestamp
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicine_categories
                alter column updated_at set default current_timestamp
                """);

        jdbcTemplate.execute("""
                update public.medicine_categories
                set is_active = true
                where is_active is null
                """);

        jdbcTemplate.execute("""
                update public.medicine_categories
                set created_at = current_timestamp
                where created_at is null
                """);

        jdbcTemplate.execute("""
                update public.medicine_categories
                set updated_at = current_timestamp
                where updated_at is null
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicine_categories
                alter column name set not null
                """);
    }

    private void ensureLegacyMedicineColumns() {
        jdbcTemplate.execute("""
                alter table if exists public.medicines
                add column if not exists medicine_category varchar(100) default 'Khác'
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicines
                add column if not exists category varchar(100)
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicines
                alter column medicine_category set default 'Khác'
                """);

        jdbcTemplate.execute("""
                update public.medicines
                set medicine_category = coalesce(
                    nullif(btrim(cast(medicine_category as text)), ''),
                    nullif(btrim(cast(category as text)), ''),
                    'Khác'
                )
                where medicine_category is null or btrim(cast(medicine_category as text)) = ''
                """);

        jdbcTemplate.execute("""
                alter table if exists public.medicines
                alter column medicine_category set not null
                """);
    }

    private void ensureMedicineCategoryForeignKeyColumn() {
        jdbcTemplate.execute("""
                alter table if exists public.medicines
                add column if not exists medicine_category_id integer
                """);
    }

    private void seedDefaultCategories() {
        for (String categoryName : DEFAULT_CATEGORY_NAMES) {
            jdbcTemplate.update("""
                    insert into public.medicine_categories (name, description, is_active, created_at, updated_at)
                    select ?, null, true, current_timestamp, current_timestamp
                    where not exists (
                        select 1 from public.medicine_categories where lower(name) = lower(?)
                    )
                    """, categoryName, categoryName);
        }
    }

    private void migrateLegacyCategoryData() {
        String resolvedCategoryExpression = resolvedCategoryExpression("");
        List<String> legacyNames = jdbcTemplate.query(
                "select distinct " + resolvedCategoryExpression + " as category_name from public.medicines",
                (rs, rowNum) -> rs.getString("category_name")
        );

        for (String legacyName : legacyNames) {
            String normalizedName = legacyName == null || legacyName.isBlank() ? DEFAULT_CATEGORY_NAME : legacyName.trim();
            jdbcTemplate.update("""
                    insert into public.medicine_categories (name, description, is_active, created_at, updated_at)
                    select ?, null, true, current_timestamp, current_timestamp
                    where not exists (
                        select 1 from public.medicine_categories where lower(name) = lower(?)
                    )
                    """, normalizedName, normalizedName);
        }

        jdbcTemplate.execute("""
                with resolved as (
                    select
                        m.id as medicine_id,
                        """
                + resolvedCategoryExpression("m")
                + """
                         as resolved_name
                    from public.medicines m
                ),
                matched as (
                    select
                        resolved.medicine_id,
                        mc.id as category_id,
                        mc.name as category_name
                    from resolved
                    join lateral (
                        select id, name
                        from public.medicine_categories
                        where lower(name) = lower(resolved.resolved_name)
                        order by id asc
                        limit 1
                    ) mc on true
                )
                update public.medicines m
                set medicine_category_id = matched.category_id,
                    medicine_category = matched.category_name,
                    category = coalesce(nullif(btrim(cast(m.category as text)), ''), matched.category_name)
                from matched
                where m.id = matched.medicine_id
                  and (
                      m.medicine_category_id is distinct from matched.category_id
                      or m.medicine_category is distinct from matched.category_name
                  )
                """);

        Integer defaultCategoryId = findCategoryId(DEFAULT_CATEGORY_NAME);
        if (defaultCategoryId != null) {
            jdbcTemplate.update("""
                    update public.medicines
                    set medicine_category_id = ?
                    where medicine_category_id is null
                    """, defaultCategoryId);
        }

        jdbcTemplate.execute("""
                update public.medicines m
                set medicine_category = mc.name,
                    category = coalesce(nullif(btrim(cast(m.category as text)), ''), mc.name)
                from public.medicine_categories mc
                where m.medicine_category_id = mc.id
                """);
    }

    private String resolvedCategoryExpression(String alias) {
        String prefix = alias == null || alias.isBlank() ? "" : alias + ".";
        String escapedDefault = DEFAULT_CATEGORY_NAME.replace("'", "''");
        return """
                coalesce(
                    nullif(
                        case
                            when nullif(btrim(cast(%1$smedicine_category as text)), '') is null then null
                            when lower(btrim(cast(%1$smedicine_category as text))) = lower('%2$s')
                                 and nullif(btrim(cast(%1$scategory as text)), '') is not null then null
                            when nullif(btrim(cast(%1$sunit as text)), '') is not null
                                 and lower(btrim(cast(%1$smedicine_category as text))) = lower(btrim(cast(%1$sunit as text)))
                                 and nullif(btrim(cast(%1$scategory as text)), '') is not null then null
                            else btrim(cast(%1$smedicine_category as text))
                        end,
                        ''
                    ),
                    nullif(btrim(cast(%1$scategory as text)), ''),
                    '%2$s'
                )
                """.formatted(prefix, escapedDefault).trim();
    }

    private Integer findCategoryId(String categoryName) {
        List<Integer> ids = jdbcTemplate.query("""
                        select id
                        from public.medicine_categories
                        where lower(name) = lower(?)
                        order by id asc
                        limit 1
                        """,
                (rs, rowNum) -> rs.getInt("id"),
                categoryName
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void ensureMedicineCategoryForeignKey() {
        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'fk_medicine_category'
                    ) then
                        alter table public.medicines
                        add constraint fk_medicine_category
                        foreign key (medicine_category_id)
                        references public.medicine_categories(id)
                        on delete set null;
                    end if;
                end $$;
                """);
    }

    private void ensureIndexes() {
        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_indexes
                        where schemaname = 'public'
                          and tablename = 'medicines'
                          and indexname = 'idx_medicines_category'
                    ) then
                        create index idx_medicines_category on public.medicines(medicine_category);
                    end if;
                end $$;
                """);

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_indexes
                        where schemaname = 'public'
                          and tablename = 'medicines'
                          and indexname = 'idx_medicines_category_id'
                    ) then
                        create index idx_medicines_category_id on public.medicines(medicine_category_id);
                    end if;
                end $$;
                """);
    }

    private void migrateMedicineTextColumnsFromByteaIfNeeded() {
        jdbcTemplate.execute("""
                do $$
                begin
                    if exists (
                        select 1
                        from information_schema.columns
                        where table_schema = 'public'
                          and table_name = 'medicines'
                          and column_name = 'name'
                          and udt_name = 'bytea'
                    ) then
                        begin
                            alter table public.medicines
                            alter column name type varchar(100)
                            using convert_from(name, 'UTF8');
                        exception when others then
                            alter table public.medicines
                            alter column name type varchar(100)
                            using encode(name, 'escape');
                        end;
                    end if;

                    if exists (
                        select 1
                        from information_schema.columns
                        where table_schema = 'public'
                          and table_name = 'medicines'
                          and column_name = 'unit'
                          and udt_name = 'bytea'
                    ) then
                        begin
                            alter table public.medicines
                            alter column unit type varchar(20)
                            using convert_from(unit, 'UTF8');
                        exception when others then
                            alter table public.medicines
                            alter column unit type varchar(20)
                            using encode(unit, 'escape');
                        end;
                    end if;

                    if exists (
                        select 1
                        from information_schema.columns
                        where table_schema = 'public'
                          and table_name = 'medicines'
                          and column_name = 'medicine_category'
                          and udt_name = 'bytea'
                    ) then
                        begin
                            alter table public.medicines
                            alter column medicine_category type varchar(100)
                            using convert_from(medicine_category, 'UTF8');
                        exception when others then
                            alter table public.medicines
                            alter column medicine_category type varchar(100)
                            using encode(medicine_category, 'escape');
                        end;
                    end if;

                    if exists (
                        select 1
                        from information_schema.columns
                        where table_schema = 'public'
                          and table_name = 'medicines'
                          and column_name = 'category'
                          and udt_name = 'bytea'
                    ) then
                        begin
                            alter table public.medicines
                            alter column category type varchar(100)
                            using convert_from(category, 'UTF8');
                        exception when others then
                            alter table public.medicines
                            alter column category type varchar(100)
                            using encode(category, 'escape');
                        end;
                    end if;
                end $$;
                """);
    }
}
