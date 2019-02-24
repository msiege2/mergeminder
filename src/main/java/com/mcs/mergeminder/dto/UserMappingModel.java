package com.mcs.mergeminder.dto;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity(name = "UserMapping")
@JsonPropertyOrder({"id", "gitlabUsername", "gitlabName", "slackUID", "slackEmail"})
public class UserMappingModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String gitlabUsername;
	private String gitlabName;
	private String slackUID;
	private String slackEmail;

	public UserMappingModel() {
		// empty constructor
	}

	public UserMappingModel(String gitlabUsername) {
		this.gitlabUsername = gitlabUsername;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getGitlabUsername() {
		return gitlabUsername;
	}

	public void setGitlabUsername(String gitlabUsername) {
		this.gitlabUsername = gitlabUsername;
	}

	public String getGitlabName() {
		return gitlabName;
	}

	public void setGitlabName(String gitlabName) {
		this.gitlabName = gitlabName;
	}

	public String getSlackUID() {
		return slackUID;
	}

	public void setSlackUID(String slackUID) {
		this.slackUID = slackUID;
	}

	public String getSlackEmail() {
		return slackEmail;
	}

	public void setSlackEmail(String slackEmail) {
		this.slackEmail = slackEmail;
	}
}
