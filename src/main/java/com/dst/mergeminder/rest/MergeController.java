package com.dst.mergeminder.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dst.mergeminder.MergeMinder;

@RestController
public class MergeController {

	@Autowired
	MergeMinder mergeMinder;

	@GetMapping("/")
	public String mind() {
		new Thread() {

			@Override public void run() {
				mergeMinder.mindMerges();
			}
		}.run();
		return "Ran minding.";
	}
}
