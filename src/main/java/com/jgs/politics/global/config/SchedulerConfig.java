package com.jgs.politics.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    
    /**
     * RestTemplate Bean 등록
     * VoteUpdateService와 VoteDataService에서 API 호출에 사용됩니다.W
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

