package com.jgs.politics.domain.politician.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jgs.politics.domain.politician.Politician;
import com.jgs.politics.domain.politician.repository.PoliticianRepository;

@RestController
@RequestMapping("/api/statistics")
public class PoliticianStatController {

    @Autowired
    private PoliticianRepository repository;

    // 1. 정당별 인원수 집계
    @GetMapping("/party")
    public Map<String, Long> getPartyDistribution() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(Politician::getPolyNm, Collectors.counting()));
    }

    // 2. 지역별(시/도 단위) 인원수 집계
    @GetMapping("/region")
    public Map<String, Long> getRegionDistribution() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(Politician::getCityName, Collectors.counting()));
    }

    // 3. 초선/재선 비율 집계
    @GetMapping("/election-count")
    public Map<String, Long> getElectionCountDistribution() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(Politician::getReeleGbnNm, Collectors.counting()));
    }
    
    // 4. 성별 비율 집계
    @GetMapping("/gender")
    public Map<String, Long> getGenderCountDistribution() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(Politician::getSexGbnNm, Collectors.counting()));
    }
    
    
}