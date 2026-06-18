package com.dhbw.webeng2.stomo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger metadata. The UI is served at {@code /swagger-ui.html} and the spec at
 * {@code /v3/api-docs}. A bearer-JWT security scheme is declared so the watchlist endpoints
 * can be tried from the "Authorize" dialog with a token from {@code /api/auth/login}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI stomoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StoMo API")
                        .description("Stock monitoring: market data, authentication and personal watchlists.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
