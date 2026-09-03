package com.company.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.TimeZone;

@SpringBootTest
class FulfillmentApplicationTests {

	@Test
	void contextLoads() {
	}

	static {
		TimeZone.setDefault(
				TimeZone.getTimeZone("Asia/Kolkata")
		);
	}

}
