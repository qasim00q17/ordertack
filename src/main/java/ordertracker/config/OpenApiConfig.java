package ordertracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI orderTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OrderTracker API")
                        .description("""
                                E-Commerce Order Management Backend with Webhook Integration.
                                
                                Use **POST /api/auth/login** to obtain a JWT token,
                                then click **Authorize** and paste it as: `Bearer <token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OrderTracker Team")
                                .email("support@ordertracker.com"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter the JWT token obtained from /api/auth/login")));
    }
}