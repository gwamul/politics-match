package com.jgs.politics.domain.vote;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteDataService {

    private final VoteHistoryRepository voteRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${na.api.key}")
    private String apiKey;

    // 1. 요청 주소 변경 (nojepdqqaweusdfbi)
    private final String VOTE_API_URL = "https://open.assembly.go.kr/portal/openapi/nojepdqqaweusdfbi";

    public void fetchVoteResults(String age, String billId) {
        String url = String.format("%s?KEY=%s&Type=json&pIndex=1&pSize=500&AGE=%s&BILL_ID=%s",
                VOTE_API_URL, apiKey, age, billId);

        try {
            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            // 2. 루트 노드 이름 변경: ALLNAPVOTE -> nojepdqqaweusdfbi
            JsonNode mainNode = root.path("nojepdqqaweusdfbi");

            // 3. 방어적 체크 (데이터가 없는 경우 처리)
            if (mainNode.isMissingNode() || mainNode.size() < 2) {
                System.out.println(">>> [데이터 없음] " + billId + " 에 대한 표결 결과가 존재하지 않습니다.");
                return;
            }

            // 4. 데이터(row) 추출
            JsonNode rows = mainNode.get(1).path("row");
            if (rows.isMissingNode()) return;

            List<VoteHistory> voteList = new ArrayList<>();
            for (JsonNode row : rows) {
                voteList.add(VoteHistory.builder()
                        .monaCd(row.path("MONA_CD").asText())
                        .hgNm(row.path("HG_NM").asText())
                        .billId(row.path("BILL_ID").asText())
                        .billName(row.path("BILL_NAME").asText())
                        .voteDate(row.path("VOTE_DATE").asText())
                        .resultVote(row.path("RESULT_VOTE_MOD").asText())
                        .polyNm(row.path("POLY_NM").asText())
                        .age(row.path("AGE").asText())
                        .build());
            }

            voteRepository.saveAll(voteList);
            System.out.println(">>> [성공] " + billId + " 데이터 " + voteList.size() + "건 저장 완료.");

        } catch (Exception e) {
            System.err.println(">>> 표결 데이터 수집 중 오류: " + e.getMessage());
        }
    }
}