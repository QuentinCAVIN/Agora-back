package com.agora.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AGORA API")
                        .description("""
                                Backend de réservation de ressources pour une mairie.

                                Authentification : Bearer JWT (`Authorization: Bearer <accessToken>`), \
                                refresh HttpOnly sur `/api/auth/refresh`.

                                Les schémas reflètent le contrat fonctionnel (endpoints, DTOs). \
                                Documentation interactive : `/swagger-ui.html`, OpenAPI JSON : `/v3/api-docs`.
                                """)
                        .version("1.0.0"));
    }
}
