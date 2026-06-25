package com.ezlearning.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
@OpenAPIDefinition(
    info = @Info(title = "EzLearning API", version = "1.0.0"),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Cole o token JWT obtido em /api/auth/login"
)
public class OpenApiConfig {

    @Bean
    public GlobalOperationCustomizer securityRequirementCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            var className = handlerMethod.getBeanType().getSimpleName();
            if (className.equals("AuthController") || className.equals("HealthController")) {
                return operation;
            }
            operation.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                    .addList("bearerAuth"));
            return operation;
        };
    }
}
