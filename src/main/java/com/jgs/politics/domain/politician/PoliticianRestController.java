package com.jgs.politics.domain.politician;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/politicians")
public class PoliticianRestController {

    @Autowired
    private PoliticianService politicianService;

    // 정 경 사 젠더 환경 외교 ... 
    
    @GetMapping("/search")
    public ResponseEntity<List<PoliticianSummaryDTO>> searchPoliticians(
            @RequestParam(required = false) String name,   // 이름 파라미터 추가
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String party,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String reele) {
        
        List<PoliticianSummaryDTO> list = politicianService.getFilteredPoliticians(name, city, party, gender, reele);
        return ResponseEntity.ok(list);
    }
    
    @GetMapping("/detail/{monaCd}")
    public ResponseEntity<Politician> getDetail(@PathVariable String monaCd) {
        Politician politician = politicianService.getPoliticianDetail(monaCd);
        return ResponseEntity.ok(politician);
    }
    
}