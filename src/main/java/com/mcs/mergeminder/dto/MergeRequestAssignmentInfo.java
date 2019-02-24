package com.mcs.mergeminder.dto;

import java.util.Date;

import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Note;
import org.gitlab4j.api.models.User;

public class MergeRequestAssignmentInfo {

	private MergeRequest mr;
	private User assignee;
	private User author;
	private Note lastAssignmentNote;
	private String projectNamespace;
	private String projectName;

	public MergeRequestAssignmentInfo(MergeRequest mr, User assignee, User author, Note lastAssignmentNote, String projectNamespace, String projectName) {
		this.mr = mr;
		this.assignee = assignee;
		this.author = author;
		this.lastAssignmentNote = lastAssignmentNote;
		this.projectNamespace = projectNamespace;
		this.projectName = projectName;
	}

	public MergeRequest getMr() {
		return mr;
	}

	public User getAssignee() {
		return assignee;
	}

	public User getAuthor() {
		return author;
	}

	public Date getAssignedAt() {
		return lastAssignmentNote == null ? getMr().getCreatedAt() : lastAssignmentNote.getCreatedAt();
	}

	public Integer getLastAssignmentId() {
		return lastAssignmentNote == null ? -1 : lastAssignmentNote.getId();
	}

	public String getProjectNamespace() {
		return projectNamespace;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getFullyQualifiedProjectName() {
		return projectNamespace + "/" + projectName;
	}
}
