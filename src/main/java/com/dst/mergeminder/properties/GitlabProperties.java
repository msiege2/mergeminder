package com.dst.mergeminder.properties;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@PropertySource("classpath:gitlab.properties")
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "mm.gitlab")
@Validated
public class GitlabProperties {

	/**
	 * Gitlab URL.  Must begin with http:// or https://
	 */
	@NotNull
	@Pattern(regexp = "^(?i)https?://.*")
	private String url;
	@NotNull
	private String accesstoken;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAccesstoken() {
		return accesstoken;
	}

	public void setAccesstoken(String accesstoken) {
		this.accesstoken = accesstoken;
	}
}
