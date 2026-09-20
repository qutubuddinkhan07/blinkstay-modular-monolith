package blinkstay.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {
	private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

	@Bean
	public OpenAPI customOpenAPI() {

		return new OpenAPI()
				.info(new Info().title("Product Inventory System API").version("1.0")
						.description("REST APIs for Product Inventory System"))

				// Define Bearer Authentication
				.schemaRequirement(SECURITY_SCHEME_NAME, new SecurityScheme().name(SECURITY_SCHEME_NAME)
						.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
	}
}
