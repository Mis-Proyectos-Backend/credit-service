package com.banck.credit;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class CreditServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void main_shouldStartApplication() {

		try (MockedStatic<SpringApplication> mocked =
					 mockStatic(SpringApplication.class)) {


			CreditServiceApplication.main(new String[]{});


			mocked.verify(() ->
					SpringApplication.run(
							CreditServiceApplication.class,
							new String[]{}
					)
			);
		}
	}

}
