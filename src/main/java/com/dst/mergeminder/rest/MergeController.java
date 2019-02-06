package com.dst.mergeminder.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dst.mergeminder.MergeMinder;
import com.dst.mergeminder.dao.MergeMinderDb;
import com.dst.mergeminder.dto.MergeRequestModel;
import com.dst.mergeminder.dto.UserMappingModel;

@RestController
public class MergeController {

	@Autowired
	MergeMinder mergeMinder;
	@Autowired
	MergeMinderDb mergeMinderDb;

	/**
	 * Kicks off the minding process.
	 * @return
	 */
	@GetMapping("/mind")
	public String mind() {
		kickoffMind();
		return "Ran minding.";
	}

	/**
	 * Kicks off the purge process.
	 * @return
	 */
	@GetMapping("/purge")
	public String purge() {
		kickoffPurge();
		return "Ran MergePurge.";
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
		mergeMinderDb.saveUserMapping(newUserMapping);

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
		mergeMinderDb.saveUserMapping(existingUserMapping);

		return ResponseEntity.ok(existingUserMapping);

	}

	@Async
	void kickoffMind() {
		mergeMinder.mindMerges();
	}

	@Async
	void kickoffPurge() {
		mergeMinder.mergePurge();
	}

}
