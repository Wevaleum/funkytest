package com.funkytest.backend.web;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder endpoint that proves the Angular app can reach the API.
 */
@RestController
@RequestMapping("/api")
public class GreetingController {

	@GetMapping("/greeting")
	public Greeting greeting(@RequestParam(defaultValue = "world") String name) {
		return new Greeting("Hello, " + name + "!", Instant.now());
	}

}
