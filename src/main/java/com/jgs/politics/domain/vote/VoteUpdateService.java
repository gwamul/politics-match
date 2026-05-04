package com.jgs.politics.domain.vote;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.jgs.politics.domain.analysis.AnalysisSyncService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 주기적으로 Vote 데이터를 업데이트하는 서비스
 * 
 * 실행 주기:
 * - 매일 새벽 2시: 표결 결과 조회 및 업데이트 → 분석 재계산
 * - 매주 월요일 새벽 3시: 일주일간의 새로운 의안 전수 조사 → 분석 재계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteUpdateService {

    private final VoteHistoryRepository voteRepository;
    private final RestTemplate restTemplate;
    private final AnalysisSyncService analysisSyncService;

    @Value("${na.api.key}")
    private String apiKey;

    @Value("${vote.update.current-age:22}")
    private String currentAge;

    @Value("${vote.scheduler.api-delay-ms:200}")
    private long apiDelayMs;

    // 표결 상세(찬반) API
    private final String VOTE_API_URL = "https://open.assembly.go.kr/portal/openapi/nojepdqqaweusdfbi";
    // 의안 목록 API (ID 추출용)
    private final String BILL_LIST_API_URL = "https://open.assembly.go.kr/portal/openapi/TVBPMBILL11";

    /**
     * 매일 새벽 2시에 실행: 최근 표결 의안들을 수집합니다.
     * Cron 표현식: 초 분 시 일 월 요일
     * 0 0 2 * * * = 매일 새벽 2:00:00
     */
    @Scheduled(cron = "${vote.scheduler.daily.cron:0 0 2 * * *}")
    @Transactional
    public void updateDailyVotes() {
        log.info("▶ [스케줄러 실행] 일일 표결 데이터 업데이트 시작 (시각: {})", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            // 1. 최근 의안 리스트 조회
            List<String> recentBillIds = fetchRecentBillIds(currentAge);

            if (recentBillIds.isEmpty()) {
                log.warn(">>> [경고] 조회된 의안이 없습니다.");
                return;
            }

            log.info(">>> 총 {}개의 의안 발견", recentBillIds.size());

            // 2. 발견된 의안들 중 미처리 의안만 수집
            int successCount = 0;
            for (String billId : recentBillIds) {
                try {
                    fetchAndSaveVoteResults(currentAge, billId);
                    successCount++;

                    // API 서버 부하 방지
                    Thread.sleep(apiDelayMs);
                } catch (Exception e) {
                    log.error(">>> 의안ID: {} 수집 중 오류: {}", billId, e.getMessage());
                }
            }

            log.info("✔ [완료] {}건 중 {}건의 의안 데이터 수집 완료", recentBillIds.size(), successCount);

            // 데이터 수집 완료 후 분석 동기화
            log.info("▶ [분석 시작] 수집 완료 후 분석 데이터 동기화...");
            try {
                analysisSyncService.syncAnalysis();
                log.info("✔ [분석 완료] 분석 데이터 동기화 완료");
            } catch (Exception e) {
                log.error("⚠ [분석 오류] 분석 계산 중 오류 발생: {}", e.getMessage());
                log.error("   데이터는 수집되었으나 분석이 지연됩니다. 나중에 재시도합니다.");
            }

        } catch (Exception e) {
            log.error("❌ [스케줄러 오류] 일일 업데이트 실패: {}", e.getMessage());
        }
    }

    /**
     * 매주 월요일 새벽 3시에 실행: 주간 의안 전수 조사
     * Cron 표현식: 0 0 3 ? * MON
     * = 매주 월요일 새벽 3:00:00
     */
    @Scheduled(cron = "${vote.scheduler.weekly.cron:0 0 3 ? * MON}")
    @Transactional
    public void updateWeeklyVotes() {
        log.info("▶ [스케줄러 실행] 주간 표결 데이터 전수 조사 시작 (시각: {})", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            // 매주 전체 의안 목록을 다시 조회
            List<String> allBillIds = fetchAllBillIds(currentAge);

            if (allBillIds.isEmpty()) {
                log.warn(">>> [경고] 조회된 의안이 없습니다.");
                return;
            }

            log.info(">>> 주간 전수조사: 총 {}개의 의안", allBillIds.size());

            int successCount = 0;
            for (String billId : allBillIds) {
                try {
                    // 이미 존재하는 의안은 스킵
                    if (!voteRepository.existsByBillId(billId)) {
                        fetchAndSaveVoteResults(currentAge, billId);
                        successCount++;
                    }

                    Thread.sleep(apiDelayMs);
                } catch (Exception e) {
                    log.error(">>> 의안ID: {} 수집 중 오류: {}", billId, e.getMessage());
                }
            }

            log.info("✔ [주간 완료] {}건 중 {}건의 신규 의안 데이터 수집", allBillIds.size(), successCount);

            // 데이터 수집 완료 후 분석 동기화
            log.info("▶ [분석 시작] 수집 완료 후 분석 데이터 동기화...");
            try {
                analysisSyncService.syncAnalysis();
                log.info("✔ [분석 완료] 분석 데이터 동기화 완료");
            } catch (Exception e) {
                log.error("⚠ [분석 오류] 분석 계산 중 오류 발생: {}", e.getMessage());
                log.error("   데이터는 수집되었으나 분석이 지연됩니다. 나중에 재시도합니다.");
            }

        } catch (Exception e) {
            log.error("❌ [스케줄러 오류] 주간 업데이트 실패: {}", e.getMessage());
        }
    }

    /**
     * 최근 의안 목록 조회 (가장 최근 페이지만)
     */
    private List<String> fetchRecentBillIds(String age) {
        String url = String.format("%s?KEY=%s&Type=json&pIndex=1&pSize=100&AGE=%s",
                BILL_LIST_API_URL, apiKey, age);

        List<String> billIds = new ArrayList<>();
        try {
            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode rows = root.path("TVBPMBILL11").get(1).path("row");

            for (JsonNode row : rows) {
                if (!row.path("PROC_RESULT").asText().equals("null")) {
                    billIds.add(row.path("BILL_ID").asText());
                }
            }

            log.debug(">>> 조회된 의안 ID (최근): {}", billIds.size());

        } catch (Exception e) {
            log.error(">>> 의안 목록 조회 실패: {}", e.getMessage());
        }
        return billIds;
    }

    /**
     * 전체 의안 목록 조회 (페이지네이션 전체)
     */
    private List<String> fetchAllBillIds(String age) {
        List<String> billIds = new ArrayList<>();
        int pIndex = 1;
        int pSize = 100;
        boolean hasMore = true;

        try {
            while (hasMore) {
                String url = String.format("%s?KEY=%s&Type=json&pIndex=%d&pSize=%d&AGE=%s",
                        BILL_LIST_API_URL, apiKey, pIndex, pSize, age);

                String response = restTemplate.getForObject(url, String.class);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);
                JsonNode rows = root.path("TVBPMBILL11").get(1).path("row");

                if (rows.isMissingNode() || !rows.isArray() || rows.size() == 0) {
                    hasMore = false;
                    break;
                }

                for (JsonNode row : rows) {
                    if (!row.path("PROC_RESULT").asText().equals("null")) {
                        billIds.add(row.path("BILL_ID").asText());
                    }
                }

                log.debug(">>> 페이지 {}: {} 건의 의안 조회", pIndex, rows.size());

                pIndex++;
                Thread.sleep(300);

            }

            log.info(">>> 전체 의안 ID 조회 완료: {}", billIds.size());

        } catch (Exception e) {
            log.error(">>> 전체 의안 목록 조회 실패: {}", e.getMessage());
        }

        return billIds;
    }

    /**
     * 특정 의안의 표결 결과 조회 및 DB 저장
     */
    @Transactional
    private void fetchAndSaveVoteResults(String age, String billId) {
        // 이미 수집된 의안 스킵
        if (voteRepository.existsByBillId(billId)) {
            log.debug(">>> 의안ID: {} (이미 수집됨 - 스킵)", billId);
            return;
        }

        String url = String.format("%s?KEY=%s&Type=json&pIndex=1&pSize=500&AGE=%s&BILL_ID=%s",
                VOTE_API_URL, apiKey, age, billId);

        try {
            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            JsonNode mainNode = root.path("nojepdqqaweusdfbi");
            if (mainNode.isMissingNode() || mainNode.size() < 2) {
                log.warn(">>> 의안ID: {} - API 응답에 데이터가 없음", billId);
                return;
            }

            JsonNode rows = mainNode.get(1).path("row");
            if (rows.isMissingNode() || !rows.isArray()) {
                log.warn(">>> 의안ID: {} - row 노드가 배열이 아님", billId);
                return;
            }

            List<VoteHistory> voteList = new ArrayList<>();
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

            if (!voteList.isEmpty()) {
                voteRepository.saveAll(voteList);
                log.info(">>> 의안: {} - {}건 저장 완료", billId, voteList.size());
            } else {
                log.warn(">>> 의안ID: {} - 파싱된 데이터가 0건", billId);
            }

        } catch (Exception e) {
            log.error(">>> 의안ID: {} 수집 중 오류: {}", billId, e.getMessage());
        }
    }
}
