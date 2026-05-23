package com.xavopls.ibkr_dashboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ibkrDashboardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("IBKR Dashboard API")
                        .version("1.0.0")
                        .description("""
                                Synchronizes Interactive Brokers account data into PostgreSQL for Grafana dashboards.
                                Use /sync for the normal daily refresh, /sync/history for one-time backfills, and
                                the narrower endpoints for debugging specific sync stages.
                                """)
                        .license(new License().name("Private project")));
    }
}
