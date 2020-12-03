package com.mcs.mergeminder.dto;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("SlackUserSearchCriteria")
@JsonPropertyOrder({"id", "username", "realName", "email"})
public class SlackUserSearchCriteria {

	private String id;
	private String username;
	private String realName;
	private String email;

	public SlackUserSearchCriteria() {
		// empty constructor
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getRealName() {
		return this.realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isEmptyCriteria() {
		return !StringUtils.hasLength(this.id)
			&& !StringUtils.hasLength(this.username)
			&& !StringUtils.hasLength(this.realName)
			&& !StringUtils.hasLength(this.email);
	}
}
