package com.funkytest.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(GreetingController.class)
class GreetingControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void greetsTheGivenName() {
		assertThat(mvc.get().uri("/api/greeting").param("name", "funkytest"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.message")
			.isEqualTo("Hello, funkytest!");
	}

}
