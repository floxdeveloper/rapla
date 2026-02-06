package org.rapla.enpoints.server;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * OpenAPI 3.0 configuration for Rapla REST API
 *
 * Provides Swagger/OpenAPI documentation for all REST endpoints.
 * Access the documentation at: /rapla/swagger-ui.html or /rapla/api-docs
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Rapla REST API",
        version = "2.1",
        description = "REST API for Rapla - Resource Scheduling and Event Planning",
        contact = @Contact(
            name = "Rapla Team",
            url = "https://github.com/rapla/rapla"
        ),
        license = @License(
            name = "Dual License: GNU AFFERO GENERAL PUBLIC LICENSE or Apache License 2.0",
            url = "https://github.com/rapla/rapla"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8051/rapla",
            description = "Local Rapla server"
        ),
        @Server(
            url = "/rapla",
            description = "Relative path"
        )
    },
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    description = "JWT Bearer token obtained from /login endpoint",
    bearerFormat = "JWT"
)
public class OpenAPIConfiguration {
    /**
     * This class serves as OpenAPI configuration holder.
     * No instances needed - it's just for annotations.
     */
}
