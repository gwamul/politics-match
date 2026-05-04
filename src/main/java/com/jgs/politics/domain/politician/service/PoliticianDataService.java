package com.jgs.politics.domain.politician.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.jgs.politics.domain.politician.Politician;
import com.jgs.politics.domain.politician.repository.PoliticianRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PoliticianDataService {

    @Autowired
    private PoliticianRepository politicianRepository;

    private final String API_KEY = "20cf57600272472aa72df90be905c3b0";
    private final String API_URL = "https://open.assembly.go.kr/portal/openapi/ALLNAMEMBER";

    public void fetchAndSaveAllData() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        int pIndex = 1;
        int pSize = 500; // 한 번에 500명씩 가져옵니다.
        int totalSaved = 0;

        System.out.println(">>> [데이터 수집 시작] 전체 역대 의원 데이터를 순회합니다...");

        try {
            while (true) {
                String url = String.format("%s?KEY=%s&Type=json&pIndex=%d&pSize=%d", API_URL, API_KEY, pIndex, pSize);
                String response = restTemplate.getForObject(url, String.class);
                
                JsonNode root = mapper.readTree(response);
                JsonNode mainNode = root.path("ALLNAMEMBER");

                if (mainNode.isMissingNode() || mainNode.get(1) == null || !mainNode.get(1).has("row")) {
                    System.out.println(">>> [수집 종료] 모든 데이터를 확인했습니다.");
                    break;
                }

                JsonNode rows = mainNode.get(1).path("row");
                List<Politician> politiciansToSave = new ArrayList<>();

                for (JsonNode row : rows) {
                    Politician p = mapper.treeToValue(row, Politician.class);

                    // [핵심 필터] 22대 당선 이력이 있는 경우에만 처리
                    if (p.getUnits() != null && p.getUnits().contains("22")) {
                        
                        // 1. 역대 중첩 데이터에서 '최신(마지막) 값'만 추출 (정당, 지역구, 선수)
                        // 예: "한나라당/새누리당/국민의힘" -> "국민의힘"
                        p.setPolyNm(getLatestValue(p.getPolyNm()));
                        p.setOrigNm(getLatestValue(p.getOrigNm()));
                        p.setReeleGbnNm(getLatestValue(p.getReeleGbnNm()));

                        // 2. 기본값 방어 로직 (정제 후 수행)
                        if (p.getPolyNm() == null || p.getPolyNm().isEmpty()) p.setPolyNm("무소속");
                        if (p.getOrigNm() == null || p.getOrigNm().isEmpty()) p.setOrigNm("비례대표");

                        // 3. 지역구 세부 정제 (아까 만든 로직)
                        p.setRegionName(refineRegionName(p.getOrigNm()));
                        p.setCityName(p.getRegionName().split(" ")[0]);

                        // 4. 사진 URL 보정
                        if (p.getPhotoUrl() == null || p.getPhotoUrl().isEmpty()) {
                            p.setPhotoUrl("https://www.assembly.go.kr/photo/" + p.getMonaCd() + ".jpg");
                        }
                        
                        politiciansToSave.add(p);
                    }
                }

                if (!politiciansToSave.isEmpty()) {
                    politicianRepository.saveAll(politiciansToSave);
                    totalSaved += politiciansToSave.size();
                    System.out.println(">>> [진행 중] " + pIndex + "페이지 처리 완료 (" + politiciansToSave.size() + "명 선별)");
                }

                pIndex++;
            }
            System.out.println(">>> [최종 완료] 정제된 22대 국회의원 총 " + totalSaved + "명 저장 완료.");

        } catch (Exception e) {
            System.err.println(">>> [치명적 오류] " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String refineRegionName(String origNm) {
        if (origNm == null || origNm.isEmpty()) return "정보없음";
        if (origNm.equals("비례대표")) return "비례대표";
        
        // '/'가 있으면 마지막 항목 추출, 없으면 전체 사용
        String[] parts = origNm.split("/");
        String lastPart = parts[parts.length - 1].trim();
        
        return lastPart;
    }
    
    private String getLatestValue(String value) {
        if (value == null || value.isEmpty()) return value;
        
        // '/', ',', ' ' 등이 섞여 있을 수 있으므로 정규식으로 분리 후 마지막 요소 선택
        String[] parts = value.split("[/|,]");
        return parts[parts.length - 1].trim();
    }
}