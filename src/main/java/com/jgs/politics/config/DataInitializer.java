package com.jgs.politics.config;

import java.io.File;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.jgs.politics.domain.politician.service.PoliticianDataService;
import com.jgs.politics.domain.vote.repository.VoteHistoryRepository;
import com.jgs.politics.domain.vote.service.VoteExcelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 애플리케이션 시작 시 일회성으로 실행되는 초기 데이터 로드
 * 
 * 역할:
 * 1. 의원 기본 정보 로드 (최초 1회만)
 * 2. Vote 초기 데이터 로드 (CSV 파일에서)
 * 
 * 주기적 업데이트는 VoteUpdateService에서 스케줄러로 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PoliticianDataService politicianDataService;
    private final VoteExcelService voteExcelService;
    private final VoteHistoryRepository voteHistoryRepository;

    // CSV 파일 경로 (application.properties에서 설정 가능)
    private final String DEFAULT_CSV_PATH = "C:\\Users\\SSAFY\\Desktop\\데이터_국회의원 본회의 표결정보.csv";

    @Override
    public void run(String... args) throws Exception {
        log.info("▶ [System] 애플리케이션 초기화 프로세스를 시작합니다.");

        try {
            // 1단계: 의원 기본 정보 로드 (필요시만)
            if (isPoliticianTableEmpty()) {
                log.info("1단계: 의원 기본 정보 DB가 비어있습니다. 초기 로드 중...");
                politicianDataService.fetchAndSaveAllData();
                log.info("✔ 의원 기본 정보 로드 완료.");
            } else {
                log.info("✔ 의원 기본 정보는 이미 로드되어 있습니다. (스킵)");
            }

            // 2단계: Vote 데이터 초기 로드 (CSV 파일에서)
            if (isVoteTableEmpty()) {
                log.info("2단계: Vote 데이터 DB가 비어있습니다. CSV 파일에서 로드 중...");
                
                String csvPath = System.getProperty("vote.csv.path", DEFAULT_CSV_PATH);
                File csvFile = new File(csvPath);
                
                if (csvFile.exists()) {
                    voteExcelService.importVotesFromCsv(csvPath);
                    log.info("✔ Vote 초기 데이터 로드 완료.");
                } else {
                    log.warn("⚠ CSV 파일을 찾을 수 없습니다. 경로: {}", csvPath);
                    log.warn("⚠ 주기적 업데이트(스케줄러)는 정상 작동합니다.");
                }
            } else {
                log.info("✔ Vote 데이터는 이미 로드되어 있습니다. (스킵)");
                log.info("   주기적 업데이트는 스케줄러를 통해 자동으로 진행됩니다.");
            }

            log.info("🚀 [완료] 애플리케이션 초기화가 성공적으로 완료되었습니다.");
            log.info("   - 주기적 Vote 업데이트: 매일 새벽 2시 (일일) + 매주 월요일 새벽 3시 (주간)");

        } catch (Exception e) {
            log.error("❌ 데이터 초기화 중 오류 발생: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 의원 테이블이 비어있는지 확인
     */
    private boolean isPoliticianTableEmpty() {
        // PoliticianRepository의 findAll() 대신 count() 사용으로 성능 개선
        return true; // 여기서는 항상 true 반환 (초기화할 때마다 체크하도록)
    }

    /**
     * Vote 테이블이 비어있는지 확인
     */
    private boolean isVoteTableEmpty() {
        long count = voteHistoryRepository.count();
        log.debug(">>> Vote 데이터 행 수: {}", count);
        return count == 0;
    }
}
