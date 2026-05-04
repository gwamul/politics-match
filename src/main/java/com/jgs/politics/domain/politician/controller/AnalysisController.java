package com.jgs.politics.domain.politician.controller;

import com.jgs.politics.domain.analysis.repository.AnalysisSyncStateRepository;
import com.jgs.politics.domain.analysis.service.AnalysisSyncService;
import com.jgs.politics.domain.analysis.service.PoliticalRecommendationService;
import com.jgs.politics.domain.analysis.dto.PoliticalVisualizationResponseDTO;
import com.jgs.politics.domain.analysis.dto.WeightedNominateResponseDTO;
import com.jgs.politics.domain.politician.PartyAnalysis;
import com.jgs.politics.domain.politician.PoliticianProfile;
import com.jgs.politics.domain.politician.repository.PartyAnalysisRepository;
import com.jgs.politics.domain.politician.repository.PoliticianProfileRepository;
import com.jgs.politics.domain.vote.repository.VoteHistoryRepository;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

/**
 * 의원 프로필 및 정당 분석 데이터 조회 API
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final PoliticianProfileRepository politicianProfileRepository;
    private final PartyAnalysisRepository partyAnalysisRepository;
    private final VoteHistoryRepository voteHistoryRepository;
    private final AnalysisSyncService analysisSyncService;
    private final AnalysisSyncStateRepository analysisSyncStateRepository;
    private final PoliticalRecommendationService politicalRecommendationService;

    /**
     * 시스템 데이터 상태 확인
     */
    @GetMapping("/status")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        long voteCount = voteHistoryRepository.count();
        long politicianProfileCount = politicianProfileRepository.count();
        long partyAnalysisCount = partyAnalysisRepository.count();
        long lastProcessedVoteHistoryId = analysisSyncStateRepository.findById("MAIN")
                .map(state -> state.getLastProcessedVoteHistoryId() == null ? 0L : state.getLastProcessedVoteHistoryId())
                .orElse(0L);
        
        status.put("voteHistoryCount", voteCount);
        status.put("politicianProfileCount", politicianProfileCount);
        status.put("partyAnalysisCount", partyAnalysisCount);
        status.put("lastProcessedVoteHistoryId", lastProcessedVoteHistoryId);
        status.put("message", voteCount > 0 ? "✅ 데이터가 있습니다" : "⚠️ VoteHistory 데이터가 없습니다");
        
        return status;
    }

    /**
     * 분석 데이터 수동 생성 (테스트용)
     * GET /api/analysis/trigger
     * POST /api/analysis/trigger
     */
    @RequestMapping(value = "/trigger", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, String> triggerAnalysis() {
        Map<String, String> result = new HashMap<>();
        
        long voteCount = voteHistoryRepository.count();
        if (voteCount == 0) {
            result.put("status", "FAILED");
            result.put("message", "VoteHistory 데이터가 없습니다. 먼저 데이터를 로드하세요.");
            return result;
        }
        
        try {
            log.info("▶ [수동 트리거] 분석 시작...");
            analysisSyncService.syncAnalysis();
            
            long profileCount = politicianProfileRepository.count();
            
            result.put("status", "SUCCESS");
            result.put("message", "✅ 분석 완료!");
            result.put("analyzedPoliticians", String.valueOf(profileCount));
            result.put("voteRecords", String.valueOf(voteCount));
            
            log.info("✔ [완료] 분석 데이터 생성 완료");
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "❌ 분석 중 오류: " + e.getMessage());
            log.error("분석 실패: ", e);
        }
        
        return result;
    }

    /**
     * 모든 의원 프로필 조회
     */
    @GetMapping("/politicians/profiles")
    public List<PoliticianProfile> getAllPoliticianProfiles() {
        return politicianProfileRepository.findAllByOrderByApprovalRateDesc();
    }

    /**
     * 특정 의원 프로필 조회
     */
    @GetMapping("/politicians/profiles/{monaCd}")
    public PoliticianProfile getPoliticianProfile(@PathVariable String monaCd) {
        return politicianProfileRepository.findByMonaCd(monaCd)
                .orElseThrow(() -> new RuntimeException("의원 프로필을 찾을 수 없습니다: " + monaCd));
    }

    /**
     * 특정 정당 의원들 프로필 조회
     */
    @GetMapping("/politicians/profiles/party/{partyName}")
    public List<PoliticianProfile> getPoliticiansByParty(@PathVariable String partyName) {
        return politicianProfileRepository.findByPolyNm(partyName);
    }

    /**
     * 모든 정당 분석 조회
     */
    @GetMapping("/parties/analysis")
    public List<PartyAnalysis> getAllPartyAnalysis() {
        return partyAnalysisRepository.findAllByOrderByCohesionDesc();
    }

    /**
     * 특정 정당 분석 조회
     */
    @GetMapping("/parties/analysis/{partyName}")
    public PartyAnalysis getPartyAnalysis(@PathVariable String partyName) {
        return partyAnalysisRepository.findByPartyName(partyName)
                .orElseThrow(() -> new RuntimeException("정당 분석 데이터를 찾을 수 없습니다: " + partyName));
    }

    /**
     * 가중 추천(Weighted Nominate) 결과 조회
     */
    @GetMapping("/weighted-nominate")
    public WeightedNominateResponseDTO getWeightedNominate(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0.50") BigDecimal stanceWeight,
            @RequestParam(defaultValue = "0.30") BigDecimal consistencyWeight,
            @RequestParam(defaultValue = "0.20") BigDecimal activityWeight) {

        return politicalRecommendationService.getWeightedNominate(page, size, stanceWeight, consistencyWeight, activityWeight);
    }

    /**
     * 정치 성향 시각화 데이터 조회
     */
    @GetMapping("/visualization/political-map")
    public PoliticalVisualizationResponseDTO getPoliticalMap(
            @RequestParam(defaultValue = "0.50") BigDecimal stanceWeight,
            @RequestParam(defaultValue = "0.30") BigDecimal consistencyWeight,
            @RequestParam(defaultValue = "0.20") BigDecimal activityWeight) {

        return politicalRecommendationService.getVisualization(stanceWeight, consistencyWeight, activityWeight);
    }
}
