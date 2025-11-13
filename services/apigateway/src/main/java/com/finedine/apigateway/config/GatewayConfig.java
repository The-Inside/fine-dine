package com.finedine.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    private static final String AUTH_SERVICE_URI = "http://localhost:8081";


    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth_route", r -> r.path("/api/v1/auth/**")
                        .uri(AUTH_SERVICE_URI)
                )
                .route("swagger_spec", r -> r.path("/v3/api-docs/**")
                        .uri(AUTH_SERVICE_URI)
                )
                .route("swagger_ui", r -> r.path("/swagger-ui/**")
                        .uri(AUTH_SERVICE_URI))

                .build();

    }
}