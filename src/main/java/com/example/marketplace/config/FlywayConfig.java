package com.example.marketplace.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        System.out.println("====== MANUAL FLYWAY INITIALIZATION ======");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        System.out.println("====== RUNNING MANUAL FLYWAY MIGRATION ======");
        return flyway;
    }
}
