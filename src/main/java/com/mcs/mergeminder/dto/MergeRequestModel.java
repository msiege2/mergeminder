package com.mcs.mergeminder.dto;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

@Entity(name = "MergeRequests")
@JsonRootName(value = "MindedMergeRequest")
@JsonPropertyOrder({"id", "project", "mrId"})
public class MergeRequestModel {

	@Id
	private Long id;
	private String project;
	private Long mrId;
	private String assignee;
	private String assigneeEmail;
	private Long lastReminderSentAt;
	private Long lastAssignmentId;
	private Date assignedAt;
	private Date lastUpdated;

	public MergeRequestModel() {
		// empty constructor
	}

	public MergeRequestModel(MergeRequestModel otherMrModel) {
		if (otherMrModel != null) {
			this.id = otherMrModel.getId();
			this.project = otherMrModel.getProject();
			this.mrId = otherMrModel.getMrId();
			this.assignee = otherMrModel.getAssignee();
			this.assigneeEmail = otherMrModel.getAssigneeEmail();
			this.assignedAt = otherMrModel.getAssignedAt();
			this.lastAssignmentId = otherMrModel.getLastAssignmentId();
			this.lastUpdated = null;
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * This is the fully qualified project name.  ie: "frontend/ui-projeckt-x"
	 * @return
	 */
	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public Long getMrId() {
		return mrId;
	}

	public void setMrId(Long mrId) {
		this.mrId = mrId;
	}

	public String getAssignee() {
		return assignee;
	}

	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}

	public String getAssigneeEmail() {
		return assigneeEmail;
	}

	public void setAssigneeEmail(String assigneeEmail) {
		this.assigneeEmail = assigneeEmail;
	}

	public Long getLastReminderSentAt() {
		return lastReminderSentAt;
	}

	public void setLastReminderSentAt(Long lastReminderSentAt) {
		this.lastReminderSentAt = lastReminderSentAt;
	}

	public Long getLastAssignmentId() {
		return lastAssignmentId;
	}

	public void setLastAssignmentId(Long lastAssignmentId) {
		this.lastAssignmentId = lastAssignmentId;
	}

	public Date getAssignedAt() {
		if (assignedAt == null) {
			return null;
		}
		return new Date(assignedAt.getTime());
	}

	public void setAssignedAt(Date assignedAt) {
		if (assignedAt == null) {
			this.assignedAt = null;
		} else {
			this.assignedAt = new Date(assignedAt.getTime());
		}
	}

	public Date getLastUpdated() {
		if (lastUpdated == null) {
			return null;
		}
		return new Date(lastUpdated.getTime());
	}

	public void setLastUpdated(Date lastUpdated) {
		if (lastUpdated == null) {
			this.lastUpdated = null;
		} else {
			this.lastUpdated = new Date(lastUpdated.getTime());
		}
	}

	@PreUpdate
	@PrePersist
	public void updateTimeStamps() {
		lastUpdated = new Date();
	}
}
