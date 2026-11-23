package com.itzetech.dpdms.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.IOException;

/** Runs the tests against a real, throw-away PostgreSQL, so the Flyway migrations are tested on the same database as production. */
@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfig {

    @Bean(destroyMethod = "close")
    EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.start();
    }

    @Bean
    DataSource dataSource(EmbeddedPostgres pg) {
        return pg.getPostgresDatabase();
    }
}
