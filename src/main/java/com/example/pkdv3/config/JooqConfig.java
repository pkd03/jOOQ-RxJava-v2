package com.example.pkdv3.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jooq.JooqExceptionTranslator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

/**
 * Cấu hình trung tâm cho jOOQ trong hệ thống Spring Boot.
 *
 * Cơ chế hoạt động:
 * 1. Bọc DataSource trong TransactionAwareDataSourceProxy để tích hợp với Spring @Transactional.
 * 2. Cấu hình linh hoạt SQLDialect thông qua cấu hình môi trường (spring.jooq.sql-dialect):
 *    - Môi trường Production: sử dụng SQLDialect.POSTGRES.
 *    - Môi trường Test: sử dụng SQLDialect.H2 để mô phỏng chuẩn xác các câu lệnh DDL/DML.
 * 3. Đăng ký JooqExceptionTranslator để ánh xạ SQLException sang DataAccessException của Spring.
 * 4. Cấu hình RenderNameCase và RenderQuotedNames để tương thích tối đa với PostgreSQL
 *    (tránh lỗi relation "CATEGORIES" does not exist do PostgreSQL phân biệt hoa thường khi bọc dấu ngoặc kép).
 */
@Configuration
public class JooqConfig {

    @Value("${spring.jooq.sql-dialect:POSTGRES}")
    private String sqlDialectName;

    /**
     * Bọc DataSource bên trong TransactionAwareDataSourceProxy.
     * Điều này đảm bảo jOOQ tham gia cùng Transaction ngữ cảnh (@Transactional) với Spring,
     * tự động commit hoặc rollback khi có lỗi xảy ra.
     */
    @Bean
    public DataSourceConnectionProvider connectionProvider(DataSource dataSource) {
        return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource));
    }

    /**
     * Khởi tạo bean DSLContext chính cho toàn bộ ứng dụng.
     * DSLContext là đối tượng trung tâm dùng để xây dựng các câu truy vấn SQL type-safe.
     */
    @Bean
    public DSLContext dslContext(DataSourceConnectionProvider connectionProvider) {
        DefaultConfiguration jooqConfiguration = new DefaultConfiguration();

        // Thiết lập connection provider gắn với Transaction Spring
        jooqConfiguration.set(connectionProvider);

        // Thiết lập SQL Dialect tương ứng với môi trường (POSTGRES hoặc H2)
        SQLDialect dialect;
        try {
            dialect = SQLDialect.valueOf(sqlDialectName.toUpperCase());
        } catch (Exception e) {
            dialect = SQLDialect.POSTGRES;
        }
        jooqConfiguration.set(dialect);

        // Cấu hình Settings để render identifier chữ thường, tránh lỗi case-sensitive trên PostgreSQL
        Settings settings = new Settings()
                .withRenderNameCase(RenderNameCase.LOWER)
                .withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED);
        jooqConfiguration.set(settings);

        // Đăng ký listener chuyển đổi SQLException thuần sang hierarchy DataAccessException của Spring
        jooqConfiguration.set(new DefaultExecuteListenerProvider(new JooqExceptionTranslator()));

        return new DefaultDSLContext(jooqConfiguration);
    }
}
