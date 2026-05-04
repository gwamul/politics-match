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
 * 정당별 투표 패턴을 분석하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartyAnalysisCalculator {

    private final VoteHistoryRepository voteHistoryRepository;
    private final PartyAnalysisRepository partyAnalysisRepository;
    private final PoliticianProfileRepository politicianProfileRepository;

    /**
     * 모든 정당의 분석 데이터 계산 및 저장
     */
    public void analyzeAllParties() {
        log.info("▶ [분석 시작] 정당별 투표 패턴 분석 시작");

        // 0. 기존 데이터 삭제 (중복 방지)
        long deletedCount = partyAnalysisRepository.count();
        if (deletedCount > 0) {
            log.info(">>> 기존 정당 분석 데이터 {}건 삭제 중...", deletedCount);
            partyAnalysisRepository.deleteAll();
        }

        // 1. 모든 투표 기록 조회
        List<VoteHistory> allVotes = voteHistoryRepository.findAll();

        if (allVotes.isEmpty()) {
            log.warn(">>> [경고] 투표 기록이 없습니다. 분석을 건너뜁니다.");
            return;
        }

        log.info(">>> 분석할 총 투표 기록: {}건", allVotes.size());

        // 2. 정당별로 그룹화
        Map<String, List<VoteHistory>> votesByParty = allVotes.stream()
                .collect(Collectors.groupingBy(VoteHistory::getPolyNm));

        log.info(">>> 정당 수: {}개", votesByParty.size());

        // 3. 각 정당별로 분석 계산 및 저장
        int savedCount = 0;
        for (Map.Entry<String, List<VoteHistory>> entry : votesByParty.entrySet()) {
            String partyName = entry.getKey();
            List<VoteHistory> votes = entry.getValue();

            if (votes.isEmpty()) continue;

            PartyAnalysis analysis = calculatePartyAnalysis(partyName, votes);
            partyAnalysisRepository.save(analysis);
            savedCount++;
        }

        log.info("✔ [완료] {}개 정당의 투표 패턴 분석 완료", savedCount);
    }

    /**
     * 개별 정당의 분석 데이터 계산
     */
    private PartyAnalysis calculatePartyAnalysis(String partyName, List<VoteHistory> votes) {
        // 의원 수 (중복 제거)
        int memberCount = (int) votes.stream()
                .map(VoteHistory::getMonaCd)
                .distinct()
                .count();

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

        // 평균 비율
        BigDecimal avgApprovalRate = calculatePercentage(approvalCount, totalVotes);
        BigDecimal avgDisapprovalRate = calculatePercentage(disapprovalCount, totalVotes);

        // 응집도 계산
        BigDecimal cohesion = calculateCohesion(partyName, votes, memberCount);

        return PartyAnalysis.builder()
                .partyName(partyName)
                .memberCount(memberCount)
                .totalVotes(totalVotes)
                .approvalCount(approvalCount)
                .disapprovalCount(disapprovalCount)
                .abstentionCount(abstentionCount)
                .avgApprovalRate(avgApprovalRate)
                .avgDisapprovalRate(avgDisapprovalRate)
                .cohesion(cohesion)
                .build();
    }

    /**
     * 정당 응집도 계산
     * 
     * 응집도 = (정당 내 의원들의 평균 정당 충성도)
     * = 정당 내 모든 의원의 partyConsistency 평균값
     * 
     * 높을수록 의원들이 정당 방침을 잘 따름
     */
    private BigDecimal calculateCohesion(String partyName, List<VoteHistory> votes, int memberCount) {
        // 해당 정당 의원들의 프로필 조회
        List<PoliticianProfile> profiles = politicianProfileRepository.findByPolyNm(partyName);

        if (!profiles.isEmpty()) {
            // 프로필이 있으면 평균값 계산
            BigDecimal totalConsistency = profiles.stream()
                    .map(PoliticianProfile::getPartyConsistency)
                    .filter(c -> c != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return totalConsistency.divide(BigDecimal.valueOf(profiles.size()), 2, RoundingMode.HALF_UP);
        }

        // 프로필이 없으면 직접 계산
        if (votes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<String, Integer> partyVoteCounts = new HashMap<>();
        for (VoteHistory vote : votes) {
            String result = vote.getResultVote();
            if (result != null) {
                partyVoteCounts.compute(result, (k, v) -> v == null ? 1 : v + 1);
            }
        }

        // 주류 입장 찾기
        String majorityVote = partyVoteCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (majorityVote == null) {
            return BigDecimal.ZERO;
        }

        // 정당 내 의원들이 주류 입장을 따르는 정도
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
