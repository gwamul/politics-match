package com.jgs.politics.config;

import com.jgs.politics.domain.politician.PoliticianDataService;
import com.jgs.politics.domain.vote.VoteDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PoliticianDataService politicianDataService;
    private final VoteDataService voteDataService;

    @Override
    public void run(String... args) throws Exception {
        log.info("▶ [System] 초기 데이터 수집 프로세스를 시작합니다.");

        try {
            // 1. 국회의원 기초 정보 수집 (이름, 정당, 지역구 등)
            log.info("1단계: 국회의원 기초 정보 수집 중...");
            politicianDataService.fetchAndSaveAllData(); // 기존에 만드신 메서드명 확인 필요
            log.info("✔ 국회의원 기초 정보 수집 완료.");

            // 2. 표결 데이터 수집 (특정 의안 기준)
            // 일단 테스트용 의안 ID 하나를 넣어둡니다.
            // 나중에 여러 의안을 루프 돌리거나 관리자에서 리스트로 처리하도록 확장 가능합니다.
            log.info("2단계: 주요 법안 표결 데이터 수집 중...");
            String targetBillId = "PRC_Z2Z1S0W8P1O8G2A3D1E0V2Q0X2X5J3"; // 22대 샘플 의안 ID
            voteDataService.fetchVoteResults("22", targetBillId);
            log.info("✔ 표결 데이터 수집 완료.");

            log.info("🚀 모든 초기 데이터 세팅이 성공적으로 끝났습니다.");

        } catch (Exception e) {
            log.error("❌ 데이터 초기화 중 오류 발생: {}", e.getMessage());
        }
    }
}