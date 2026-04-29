package com.jgs.politics;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.jgs.politics.domain.politician.PoliticianDataService;

@SpringBootApplication
public class PoliticsMatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(PoliticsMatchApplication.class, args);
	}
	
	// ProjectApplication.java 안에 추가
	@Bean
	CommandLineRunner init(PoliticianDataService service) {
	    return args -> {
	        //service.fetchAndSaveAllData();
	    };
	}

}
