package com.jgs.politics.domain.politician.service;

import java.util.List;
import java.util.stream.Collectors;

import com.jgs.politics.domain.politician.Politician;
import com.jgs.politics.domain.politician.dto.PoliticianSummaryDTO;
import com.jgs.politics.domain.politician.repository.PoliticianRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PoliticianService {

    @Autowired
    private PoliticianRepository repository;

    /**
     * 의원 고유 코드(mona_cd)로 상세 정보 조회
     */
    public Politician getPoliticianDetail(String monaCd) {
        return repository.findById(monaCd)
                .orElseThrow(() -> new RuntimeException("해당 의원을 찾을 수 없습니다. (ID: " + monaCd + ")"));
    }
    
    
    public List<PoliticianSummaryDTO> getFilteredPoliticians(String name, String city, String party, String gender, String reele) {
        return repository.findAll().stream()
                // 이름 검색 추가 (값이 있을 때만 포함 여부 확인)
                .filter(p -> (name == null || name.isEmpty() || p.getHgNm().contains(name)))
                .filter(p -> (city == null || city.isEmpty() || p.getCityName().equals(city)))
                .filter(p -> (party == null || party.isEmpty() || p.getPolyNm().equals(party)))
                .filter(p -> (gender == null || gender.isEmpty() || p.getSexGbnNm().equals(gender)))
                .filter(p -> (reele == null || reele.isEmpty() || p.getReeleGbnNm().equals(reele)))
                .map(p -> PoliticianSummaryDTO.builder()
                        .monaCd(p.getMonaCd())
                        .hgNm(p.getHgNm())
                        .polyNm(p.getPolyNm())
                        .cityName(p.getCityName())
                        .regionName(p.getRegionName())
                        .reeleGbnNm(p.getReeleGbnNm())
                        .photoUrl(p.getPhotoUrl())
                        .blngCmitNm(p.getBlngCmitNm())
                        .build())
                .collect(Collectors.toList());
    }
}