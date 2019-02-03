package com.dst.mergeminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = {"com.dst.mergeminder"})
public class MergeMinderApplication {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	/**
	 * Entry point of the application. Run this method to start MergeMinder with the component scan, but don't forget
	 * to add the correct tokens in application.properties file.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(MergeMinderApplication.class, args);
	}
}
