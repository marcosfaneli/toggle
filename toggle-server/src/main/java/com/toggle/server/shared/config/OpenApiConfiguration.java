package com.toggle.server.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI toggleServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Toggle Server API")
                        .description("API para gerenciamento de feature toggles e registro de clientes")
                        .version("v1")
                        .contact(new Contact().name("Toggle Platform Team"))
                        .license(new License().name("Proprietary")));
    }
}
