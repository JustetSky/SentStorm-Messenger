package com.sentstorm.messenger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MessengerServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessengerServerApplication.class, args);
	}

}
