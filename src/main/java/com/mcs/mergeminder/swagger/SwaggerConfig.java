package com.mcs.mergeminder.swagger;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

import com.mcs.mergeminder.properties.MergeMinderProperties;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

	private final MergeMinderProperties mergeMinderProperties;

	public SwaggerConfig(MergeMinderProperties mergeMinderProperties) {
		this.mergeMinderProperties = mergeMinderProperties;
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
			.apiInfo(apiInfo())
			.useDefaultResponseMessages(false)
			.consumes(Collections.singleton(MediaType.APPLICATION_JSON_VALUE))
			.produces(Collections.singleton(MediaType.APPLICATION_JSON_VALUE))
			.select()
			.apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
			.paths(PathSelectors.any())
			.build();
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder()
			.title("MergeMinder API")
			.version(mergeMinderProperties.getApplicationVersion())
			.description("REST API for controlling MergeMinder")
			.license("Apache License Version 2.0")
			.licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
			.build();
	}
}
