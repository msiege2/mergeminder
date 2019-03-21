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
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isEmptyCriteria() {
		return StringUtils.isEmpty(this.id)
			&& StringUtils.isEmpty(username)
			&& StringUtils.isEmpty(realName)
			&& StringUtils.isEmpty(email);
	}
}
