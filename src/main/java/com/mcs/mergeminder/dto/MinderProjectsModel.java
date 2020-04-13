package com.mcs.mergeminder.dto;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity(name = "MinderProjects")
@JsonPropertyOrder({"id", "namespace", "project"})
public class MinderProjectsModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String namespace;
	private String project;

	public MinderProjectsModel() {
		// empty constructor
	}

	public MinderProjectsModel(String namespace, String project) {
		this.namespace = namespace;
		this.project = project;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
}
