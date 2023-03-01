package com.mcs.mergeminder.swagger;

import com.mcs.mergeminder.properties.MergeMinderProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private final MergeMinderProperties mergeMinderProperties;

	public OpenApiConfig(MergeMinderProperties mergeMinderProperties) {
		this.mergeMinderProperties = mergeMinderProperties;
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI().info(apiInfo());
	}

	private Info apiInfo() {
		return new Info()
				.title("MergeMinder API")
				.version(mergeMinderProperties.getApplicationVersion())
				.description("REST API for controlling MergeMinder")
				.contact(apiContact())
				.license(apiLicence());
	}

	private License apiLicence() {
		return new License()
				.name("Apache License Version 2.0")
				.url("https://www.apache.org/licenses/LICENSE-2.0");
	}

	private Contact apiContact() {
		return new Contact()
				.name("Matt Siegel")
				.email("git@siegelonline.com")
				.url("https://github.com/msiege2");
	}
}
