package com.dst.mergeminder.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MergeController {

	@GetMapping("/greet/{name}")
	public String greeting(@PathVariable String name) {
		return "Hi!! " + name;
	}
}
