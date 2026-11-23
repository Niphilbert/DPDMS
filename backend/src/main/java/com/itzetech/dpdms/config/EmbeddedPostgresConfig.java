package com.itzetech.dpdms.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Development convenience: starts a real PostgreSQL inside the application so nothing has to be installed.
 * Data is kept in {@code .pgdata} next to the backend and the database listens on port 5433 by default; both can be
 * changed with {@code EMBEDDED_PG_DATA} and {@code EMBEDDED_PG_PORT} (see {@code application-embedded.yml}).
 * Activate with {@code --spring.profiles.active=embedded}.
 */
@Configuration
@Profile("embedded")
class EmbeddedPostgresConfig {

    @Bean(destroyMethod = "close")
    EmbeddedPostgres embeddedPostgres(@Value("${app.embedded.port:5433}") int port,
                                      @Value("${app.embedded.data-dir:.pgdata}") String dataDir) throws IOException {
        return EmbeddedPostgres.builder()
                .setPort(port)
                .setDataDirectory(Path.of(dataDir))
                .setCleanDataDirectory(false)
                .start();
    }

    @Bean
    DataSource dataSource(EmbeddedPostgres pg) throws Exception {
        try (var c = pg.getPostgresDatabase().getConnection(); var st = c.createStatement()) {
            var rs = st.executeQuery("select 1 from pg_database where datname = 'dpdms'");
            if (!rs.next()) {
                st.execute("create database dpdms");
            }
        }
        return pg.getDatabase("postgres", "dpdms");
    }
}
