package com.jgs.politics.domain.politician;

import com.jgs.politics.domain.vote.VoteHistory;
import com.jgs.politics.domain.vote.VoteHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 의원별 투표 프로필을 분석하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoliticianProfileAnalyzer {

    private final VoteHistoryRepository voteHistoryRepository;
    private final PoliticianProfileRepository politicianProfileRepository;
    private final PartyAnalysisRepository partyAnalysisRepository;

    /**
     * 모든 의원의 프로필 분석 및 저장
     */
    public void analyzeAllPoliticians() {
        log.info("▶ [분석 시작] 의원 프로필 분석 시작");

        // 0. 기존 데이터 삭제 (중복 방지)
        long deletedCount = politicianProfileRepository.count();
        if (deletedCount > 0) {
            log.info(">>> 기존 프로필 데이터 {}건 삭제 중...", deletedCount);
            politicianProfileRepository.deleteAll();
        }

        // 1. 모든 투표 기록 조회
        List<VoteHistory> allVotes = voteHistoryRepository.findAll();

        if (allVotes.isEmpty()) {
            log.warn(">>> [경고] 투표 기록이 없습니다. 분석을 건너뜁니다.");
            return;
        }

        log.info(">>> 분석할 총 투표 기록: {}건", allVotes.size());

        // 2. 의원별로 그룹화
        Map<String, List<VoteHistory>> votesByPolitician = allVotes.stream()
                .collect(Collectors.groupingBy(VoteHistory::getMonaCd));

        log.info(">>> 의원 수: {}명", votesByPolitician.size());

        // 3. 정당별 주류 입장 미리 계산 (나중에 정당 충성도 계산용)
        Map<String, Map<String, Integer>> partyVoteCounts = calculatePartyVoteCounts(allVotes);

        // 4. 각 의원별로 프로필 계산 및 저장
        int savedCount = 0;
        for (Map.Entry<String, List<VoteHistory>> entry : votesByPolitician.entrySet()) {
            String monaCd = entry.getKey();
            List<VoteHistory> votes = entry.getValue();

            if (votes.isEmpty()) continue;

            PoliticianProfile profile = calculatePoliticianProfile(monaCd, votes, partyVoteCounts);
            politicianProfileRepository.save(profile);
            savedCount++;
        }

        log.info("✔ [완료] {}명 의원의 프로필 분석 완료", savedCount);
    }

    /**
     * 개별 의원의 프로필 계산
     */
    private PoliticianProfile calculatePoliticianProfile(String monaCd, List<VoteHistory> votes,
                                                         Map<String, Map<String, Integer>> partyVoteCounts) {
        // 기본 정보
        VoteHistory firstVote = votes.get(0);
        String hgNm = firstVote.getHgNm();
        String polyNm = firstVote.getPolyNm();

        // 투표 통계
        int totalVotes = votes.size();
        int approvalCount = (int) votes.stream()
                .filter(v -> "찬성".equals(v.getResultVote()))
                .count();
        int disapprovalCount = (int) votes.stream()
                .filter(v -> "반대".equals(v.getResultVote()))
                .count();
        int abstentionCount = (int) votes.stream()
                .filter(v -> "기권".equals(v.getResultVote()))
                .count();

        // 비율 계산
        BigDecimal approvalRate = calculatePercentage(approvalCount, totalVotes);
        BigDecimal disapprovalRate = calculatePercentage(disapprovalCount, totalVotes);
        BigDecimal abstentionRate = calculatePercentage(abstentionCount, totalVotes);

        // 정당 충성도 계산
        BigDecimal partyConsistency = calculatePartyConsistency(polyNm, votes, partyVoteCounts);

        return PoliticianProfile.builder()
                .monaCd(monaCd)
                .hgNm(hgNm)
                .polyNm(polyNm)
                .totalVotes(totalVotes)
                .approvalCount(approvalCount)
                .disapprovalCount(disapprovalCount)
                .abstentionCount(abstentionCount)
                .approvalRate(approvalRate)
                .disapprovalRate(disapprovalRate)
                .abstentionRate(abstentionRate)
                .partyConsistency(partyConsistency)
                .build();
    }

    /**
     * 정당별 주류 입장 계산
     * (정당 충성도 계산에 사용)
     */
    private Map<String, Map<String, Integer>> calculatePartyVoteCounts(List<VoteHistory> allVotes) {
        Map<String, Map<String, Integer>> partyVoteCounts = new HashMap<>();

        for (VoteHistory vote : allVotes) {
            String party = vote.getPolyNm();
            String result = vote.getResultVote();
            
            if (party != null && result != null) {
                partyVoteCounts.computeIfAbsent(party, k -> new HashMap<>())
                        .compute(result, (k, v) -> v == null ? 1 : v + 1);
            }
        }

        return partyVoteCounts;
    }

    /**
     * 정당 충성도 계산
     * = (정당 주류 입장과 일치한 투표 수) / (전체 투표 수) * 100
     */
    private BigDecimal calculatePartyConsistency(String polyNm, List<VoteHistory> votes,
                                                 Map<String, Map<String, Integer>> partyVoteCounts) {
        if (votes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<String, Integer> partyCounts = partyVoteCounts.get(polyNm);
        if (partyCounts == null || partyCounts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 정당의 주류 입장 (가장 많이 투표한 입장)
        String majorityVote = partyCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (majorityVote == null) {
            return BigDecimal.ZERO;
        }

        // 해당 의원이 주류 입장과 일치한 횟수
        long consistentVotes = votes.stream()
                .filter(v -> majorityVote.equals(v.getResultVote()))
                .count();

        return calculatePercentage((int) consistentVotes, votes.size());
    }

    /**
     * 백분율 계산 (소수점 둘째 자리까지)
     */
    private BigDecimal calculatePercentage(int count, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
