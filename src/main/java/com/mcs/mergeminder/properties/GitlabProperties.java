package com.mcs.mergeminder.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

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

	private List<String> ignoredByLabels;

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

	public List<String> getIgnoredByLabels() {
		return ignoredByLabels;
	}

	public void setIgnoredByLabels(List<String> ignoredByLabels) {
		this.ignoredByLabels = ignoredByLabels;
	}
}
