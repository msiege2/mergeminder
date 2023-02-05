package com.mcs.mergeminder.dto;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.ullink.slack.simpleslackapi.SlackUser;

@JsonRootName("SlackUser")
@JsonPropertyOrder({"id", "username", "realName", "email"})
public class SlackUserModel {

	private String id;
	private String username;
	private String realName;
	private String email;

	public SlackUserModel() {
		// empty constructor
	}

	public SlackUserModel(SlackUser su) {
		if (su != null) {
			this.id = su.getId();
			this.username = su.getUserName();
			this.realName = su.getRealName();
			this.email = su.getUserMail();
		}
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
}
