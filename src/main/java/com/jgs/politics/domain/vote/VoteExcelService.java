package com.jgs.politics.domain.vote;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jgs.politics.domain.politician.Politician;
import com.jgs.politics.domain.politician.PoliticianRepository;
import com.opencsv.CSVReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteExcelService {

    private final VoteHistoryRepository voteRepository;
    private final PoliticianRepository politicianRepository;
    private final EntityManager entityManager;

    /**
     * CSV 데이터를 DB에 적재합니다.
     * 30만 건 처리를 위해 Batch Insert 및 Memory Management를 적용했습니다.
     */
    @Transactional
    public void importVotesFromCsv(String filePath) {
        int batchSize = 1000; 
        List<VoteHistory> voteList = new ArrayList<>();

        // 1. 의원 매칭을 위한 캐시 로드 (이름+정당 조합으로 동명이인 최소화)
        Map<String, String> politicianMap = loadPoliticianMap();
        log.info("▶ [System] 의원 매칭용 캐시 로드 완료 ({}명)", politicianMap.size());

        // 2. CSV 읽기 시작 (한글 깨짐 방지를 위해 UTF-8 명시)
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
            String[] data;
            reader.readNext(); // 헤더(첫 줄) 건너뛰기

            int count = 0;
            while ((data = reader.readNext()) != null) {
                if (data.length < 9) continue; // 데이터 부족 행 스킵

                String hgNm = data[4].trim();   // 의원명(한글)
                String polyNm = data[6].trim(); // 정당명

                // Politician 테이블에서 monaCd 매칭
                String monaCd = politicianMap.getOrDefault(hgNm + polyNm, "UNKNOWN");

                voteList.add(VoteHistory.builder()
                        .age(data[0])
                        .billName(data[2])
                        .voteDate(data[3])
                        .hgNm(hgNm)
                        .hgNmHanja(data[5])        // 누락되었던 한자명 추가
                        .polyNm(polyNm)
                        .resultVote(data[7])
                        .monaCd(monaCd)            // 매칭된 monaCd 삽입
                        .billId(extractBillId(data[8])) // URL에서 ID 추출
                        .build());

                // 3. 배치 처리 및 영속성 컨텍스트 관리
                if (++count % batchSize == 0) {
                    voteRepository.saveAll(voteList);
                    voteRepository.flush();
                    entityManager.clear(); // 30만 건 처리 시 메모리 누수 방지의 핵심
                    voteList.clear();
                    log.info(">>> [진행중] {}건 적재 완료...", count);
                }
            }

            // 남은 데이터 처리
            if (!voteList.isEmpty()) {
                voteRepository.saveAll(voteList);
            }
            log.info("🚀 [완료] 총 {}건의 데이터 전수 적재가 성공적으로 끝났습니다!", count);

        } catch (Exception e) {
            log.error("❌ [오류] 대량 적재 중 치명적 실패: {}", e.getMessage());
        }
    }

    /**
     * 이름과 정당명을 키로 하여 monaCd를 매핑하는 맵을 생성합니다.
     */
    private Map<String, String> loadPoliticianMap() {
        List<Politician> politicians = politicianRepository.findAll();
        return politicians.stream()
                .collect(Collectors.toMap(
                        p -> p.getHgNm() + p.getPolyNm(),
                        Politician::getMonaCd,
                        (existing, replacement) -> existing // 중복 시 기존값 유지
                ));
    }

    private String extractBillId(String url) {
        if (url == null || !url.contains("billId=")) return "UNKNOWN";
        try {
            return url.split("billId=")[1].split("&")[0];
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}