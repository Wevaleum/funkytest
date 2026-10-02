package com.funkytest.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
// Seeding 10k products would dominate the run time and the assertions.
@TestPropertySource(properties = "app.fixtures.enabled=false")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
