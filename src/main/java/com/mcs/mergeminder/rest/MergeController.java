package com.mcs.mergeminder.rest;

import java.util.List;

import javax.json.Json;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mcs.mergeminder.MergeMinder;
import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.MergeRequestModel;
import com.mcs.mergeminder.dto.MinderProjectsModel;
import com.mcs.mergeminder.dto.SlackUserModel;
import com.mcs.mergeminder.dto.SlackUserSearchCriteria;
import com.mcs.mergeminder.dto.UserMappingModel;
import com.mcs.mergeminder.slack.SlackIntegration;

@RestController
public class MergeController {

	@Autowired
	MergeMinder mergeMinder;
	@Autowired
	MergeMinderDb mergeMinderDb;
	@Autowired
	SlackIntegration slackIntegration;


	/**
	 * Kicks off the minding process.
	 * @return
	 */
	@GetMapping("/mind")
	public ResponseEntity<String> mind() {
		kickoffMind();
		String jsonContent = Json.createObjectBuilder()
			.add("status", "Ran merges.")
			.build()
			.toString();
		return ResponseEntity.ok(jsonContent);
	}

	/**
	 * Kicks off the purge process.
	 * @return
	 */
	@GetMapping("/purge")
	public ResponseEntity<String> purge() {
		kickoffPurge();
		String jsonContent = Json.createObjectBuilder()
			.add("status", "Ran MergePurge.")
			.build()
			.toString();
		return ResponseEntity.ok(jsonContent);
	}

	/**
	 * Gets all the merges being tracked in the database.
	 * @return
	 */
	@GetMapping("/merges")
	public List<MergeRequestModel> getAllMerges() {
		return mergeMinderDb.getAllMergeRequestModels();
	}

	/**
	 * Gets an individual merge being tracked.
	 * @param id
	 * @return
	 */
	@GetMapping("/merges/{id}")
	public MergeRequestModel getMerge(@PathVariable Integer id) {
		return mergeMinderDb.getMergeRequestModel(id);
	}

	/**
	 * Gets all the projects being tracked.
	 * @return
	 */
	@GetMapping("/projects")
	public List<MinderProjectsModel> getAllProjects() {
		return mergeMinderDb.getMinderProjects();
	}

	/**
	 * Adds a new project to be tracked.
	 * @return
	 */
	@PostMapping("/projects")
	public ResponseEntity<MinderProjectsModel> createProject(@RequestBody MinderProjectsModel newProject) {
		if (newProject == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
		newProject.setId(null);
		newProject = mergeMinderDb.saveMinderProject(newProject);

		return ResponseEntity.ok(newProject);
	}

	/**
	 * Gets all the merges being tracked in the database.
	 * @return
	 */
	@DeleteMapping("/projects/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProject(@PathVariable Integer id) {
		mergeMinderDb.removeMinderProject(id);
	}

	/**
	 * Gets all the user mapping overrides.
	 * @return
	 */
	@GetMapping("/mappings")
	public List<UserMappingModel> getAllUserMappings() {
		return mergeMinderDb.getAllUserMappings();
	}

	/**
	 * Creates a user mapping override
	 * @param newUserMapping
	 * @return
	 */
	@PostMapping("/mappings")
	public ResponseEntity<UserMappingModel> createUserMapping(@RequestBody UserMappingModel newUserMapping) {
		UserMappingModel existingUserMapping = mergeMinderDb.getUserMappingByGitlabUsername(newUserMapping.getGitlabUsername());
		if (existingUserMapping != null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
		newUserMapping.setId(null);
		newUserMapping = mergeMinderDb.saveUserMapping(newUserMapping);

		return ResponseEntity.ok(newUserMapping);
	}

	/**
	 * Updates a user mapping override.
	 * @param id
	 * @param newUserMapping
	 * @return
	 */
	@PutMapping("/mappings/{id}")
	public ResponseEntity<UserMappingModel> updateUserMapping(@PathVariable Integer id, @RequestBody UserMappingModel newUserMapping) {
		UserMappingModel existingUserMapping = mergeMinderDb.getUserMappingById(id);
		if (existingUserMapping == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
		existingUserMapping.setSlackUID(newUserMapping.getSlackUID());
		existingUserMapping.setSlackEmail(newUserMapping.getSlackEmail());
		existingUserMapping = mergeMinderDb.saveUserMapping(existingUserMapping);

		return ResponseEntity.ok(existingUserMapping);
	}

	@PostMapping("/slack-users/search")
	public ResponseEntity<List<SlackUserModel>> findSlackUsers(@RequestBody SlackUserSearchCriteria searchCriteria) {
		if (searchCriteria.isEmptyCriteria()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
		List<SlackUserModel> matchingUsers = slackIntegration.searchSlackUsers(searchCriteria);
		if (CollectionUtils.isEmpty(matchingUsers)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
		return ResponseEntity.ok(matchingUsers);
	}

	@Async
	void kickoffMind() {
		mergeMinder.doMinding();
	}

	@Async
	void kickoffPurge() {
		mergeMinder.doPurge();
	}

}
