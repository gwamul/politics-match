package com.jgs.politics.domain.vote.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.jgs.politics.domain.vote.VoteHistory;
import com.jgs.politics.domain.vote.repository.VoteHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Vote 관련 API 호출 및 데이터 처리를 담당하는 서비스
 * 
 * 주기적 업데이트는 VoteUpdateService에서 스케줄러로 관리합니다.
 * 이 클래스는 저수준 API 호출 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteDataService {

    private final VoteHistoryRepository voteRepository;
    private final RestTemplate restTemplate;

    @Value("${na.api.key}")
    private String apiKey;

    // API URLs
    private final String VOTE_API_URL = "https://open.assembly.go.kr/portal/openapi/nojepdqqaweusdfbi";
    private final String BILL_LIST_API_URL = "https://open.assembly.go.kr/portal/openapi/TVBPMBILL11";

    /**
     * 의안 목록 조회 (페이지네이션)
     * 
     * @param age 국회 대수 (예: "22")
     * @param pIndex 페이지 인덱스 (기본값: 1)
     * @param pSize 페이지 크기 (기본값: 100)
     * @return 의안 ID 리스트
     */
    public List<String> fetchBillIds(String age, int pIndex, int pSize) {
        String url = String.format("%s?KEY=%s&Type=json&pIndex=%d&pSize=%d&AGE=%s",
                BILL_LIST_API_URL, apiKey, pIndex, pSize, age);

        List<String> billIds = new ArrayList<>();
        try {
            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode rows = root.path("TVBPMBILL11").get(1).path("row");

            for (JsonNode row : rows) {
                // 본회의 처리가 완료된 의안만 추출
                if (!row.path("PROC_RESULT").asText().equals("null")) {
                    billIds.add(row.path("BILL_ID").asText());
                }
            }

            log.debug(">>> 의안 목록 조회 완료: {} 건", billIds.size());

        } catch (Exception e) {
            log.error(">>> 의안 목록 조회 실패: {}", e.getMessage());
        }

        return billIds;
    }

    /**
     * 특정 의안의 표결 결과 조회
     * 
     * @param age 국회 대수
     * @param billId 의안 ID
     * @return VoteHistory 리스트
     */
    public List<VoteHistory> fetchVoteResults(String age, String billId) {
        List<VoteHistory> voteList = new ArrayList<>();

        String url = String.format("%s?KEY=%s&Type=json&pIndex=1&pSize=500&AGE=%s&BILL_ID=%s",
                VOTE_API_URL, apiKey, age, billId);

        try {
            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            JsonNode mainNode = root.path("nojepdqqaweusdfbi");
            if (mainNode.isMissingNode() || mainNode.size() < 2) {
                log.warn(">>> 의안ID: {} - API 응답에 데이터가 없음", billId);
                return voteList;
            }

            JsonNode rows = mainNode.get(1).path("row");
            if (rows.isMissingNode() || !rows.isArray()) {
                log.warn(">>> 의안ID: {} - row 노드가 배열이 아님", billId);
                return voteList;
            }

            for (JsonNode row : rows) {
                voteList.add(VoteHistory.builder()
                        .hgNm(row.path("HG_NM").asText())
                        .resultVote(row.path("RESULT_VOTE_MOD").asText())
                        .billId(row.path("BILL_ID").asText())
                        .billName(row.path("BILL_NAME").asText())
                        .polyNm(row.path("POLY_NM").asText())
                        .monaCd(row.path("MONA_CD").asText())
                        .age(age)
                        .build());
            }

            log.info(">>> 의안ID: {} - {}건 조회 완료", billId, voteList.size());

        } catch (Exception e) {
            log.error(">>> 의안ID: {} 조회 중 오류: {}", billId, e.getMessage());
        }

        return voteList;
    }

    /**
     * 표결 결과를 DB에 저장
     * 
     * @param voteList 저장할 VoteHistory 리스트
     * @return 저장된 건수
     */
    public int saveVoteResults(List<VoteHistory> voteList) {
        if (voteList == null || voteList.isEmpty()) {
            return 0;
        }

        try {
            voteRepository.saveAll(voteList);
            log.debug(">>> {}건의 표결 데이터 저장 완료", voteList.size());
            return voteList.size();
        } catch (Exception e) {
            log.error(">>> 표결 데이터 저장 중 오류: {}", e.getMessage());
            return 0;
        }
    }
}
