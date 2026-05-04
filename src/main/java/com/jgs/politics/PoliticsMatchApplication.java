package com.jgs.politics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PoliticsMatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(PoliticsMatchApplication.class, args);
	}

}
