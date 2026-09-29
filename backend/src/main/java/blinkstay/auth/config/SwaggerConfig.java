package blinkstay.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	private static final String SECURITY_SCHEME_NAME = "Cookie Authentication";

	@Bean
	public OpenAPI customOpenAPI() {

		return new OpenAPI()
				.info(new Info().title("Blinkstay System API").version("1.0")
						.description("REST APIs for Blinkstay System"))

				.schemaRequirement(SECURITY_SCHEME_NAME,
						new SecurityScheme().name("BLINKSTAY_TOKEN").type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.COOKIE)
								.description("JWT authentication using the BLINKSTAY_TOKEN HttpOnly cookie"));
	}
}