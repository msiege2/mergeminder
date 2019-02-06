package com.dst.mergeminder.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dst.mergeminder.MergeMinder;

@RestController
public class MergeController {

	@Autowired
	MergeMinder mergeMinder;

	@GetMapping("/mind")
	public String mind() {
		kickoffMind();
		return "Ran minding.";
	}

	@GetMapping("/purge")
	public String purge() {
		kickoffPurge();
		return "Ran MergePurge.";
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
