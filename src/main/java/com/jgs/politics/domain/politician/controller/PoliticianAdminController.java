package com.jgs.politics.domain.politician.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jgs.politics.domain.politician.service.PoliticianDataService;

@RestController
@RequestMapping("/admin/api")
public class PoliticianAdminController {

    @Autowired
    private PoliticianDataService politicianDataService;

    // POST 방식으로 요청할 때만 데이터를 동기화합니다.
    @PostMapping("/sync-data")
    public String syncData() {
        politicianDataService.fetchAndSaveAllData();
        return "22대 국회의원 데이터 동기화 완료 (306명)";
    }
}