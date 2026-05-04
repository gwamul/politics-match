package com.jgs.politics.domain.bill;

import com.jgs.politics.domain.bill.repository.BillAnalysisRepository;
import com.jgs.politics.domain.vote.VoteHistory;
import com.jgs.politics.domain.vote.repository.VoteHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 안건별 투표 패턴을 분석하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillAnalysisCalculator {

    private final VoteHistoryRepository voteHistoryRepository;
    private final BillAnalysisRepository billAnalysisRepository;

    /**
     * 모든 안건의 분석 데이터 계산 및 저장
     */
    public void analyzeAllBills() {
        log.info("▶ [분석 시작] 안건별 투표 패턴 분석 시작");

        // 0. 기존 데이터 삭제 (중복 방지)
        long deletedCount = billAnalysisRepository.count();
        if (deletedCount > 0) {
            log.info(">>> 기존 안건 분석 데이터 {}건 삭제 중...", deletedCount);
            billAnalysisRepository.deleteAll();
        }

        // 1. 모든 투표 기록 조회
        List<VoteHistory> allVotes = voteHistoryRepository.findAll();

        if (allVotes.isEmpty()) {
            log.warn(">>> [경고] 투표 기록이 없습니다. 분석을 건너뜁니다.");
            return;
        }

        log.info(">>> 분석할 총 투표 기록: {}건", allVotes.size());

        // 2. 안건별로 그룹화
        Map<String, List<VoteHistory>> votesByBill = allVotes.stream()
                .collect(Collectors.groupingBy(VoteHistory::getBillId));

        log.info(">>> 안건 수: {}개", votesByBill.size());

        // 3. 각 안건별로 분석 계산 및 저장
        int savedCount = 0;
        for (Map.Entry<String, List<VoteHistory>> entry : votesByBill.entrySet()) {
            String billId = entry.getKey();
            List<VoteHistory> votes = entry.getValue();

            if (votes.isEmpty()) continue;

            BillAnalysis analysis = calculateBillAnalysis(billId, votes);
            billAnalysisRepository.save(analysis);
            savedCount++;
        }

        log.info("✔ [완료] {}개 안건의 투표 패턴 분석 완료", savedCount);
    }

    /**
     * 개별 안건의 분석 데이터 계산
     */
    private BillAnalysis calculateBillAnalysis(String billId, List<VoteHistory> votes) {
        VoteHistory firstVote = votes.get(0);
        String billName = firstVote.getBillName();
        String voteDate = firstVote.getVoteDate();

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

        // 논란 지수 계산
        BigDecimal controversyScore = calculateControversyScore(approvalCount, disapprovalCount, 
                                                               abstentionCount, totalVotes);

        // 처리 결과 (가결/부결은 찬성이 많으면 가결)
        String passingStatus = approvalCount > disapprovalCount ? "가결" : 
                               disapprovalCount > approvalCount ? "부결" : "동수";

        return BillAnalysis.builder()
                .billId(billId)
                .billName(billName != null ? billName : "")
                .totalVotes(totalVotes)
                .approvalCount(approvalCount)
                .disapprovalCount(disapprovalCount)
                .abstentionCount(abstentionCount)
                .approvalRate(approvalRate)
                .disapprovalRate(disapprovalRate)
                .controversyScore(controversyScore)
                .voteDate(voteDate != null ? voteDate : "")
                .passingStatus(passingStatus)
                .build();
    }

    /**
     * 논란 지수 계산
     * 
     * controversyScore = 100 - (최다 의견 / 총 투표 수 * 100)
     * 
     * 범위: 0 ~ 100
     * - 0: 전원이 같은 입장 (논란 없음)
     * - 50: 찬성과 반대가 반반 (최고 논란)
     * - 100: 이론상 불가능 (50을 넘을 수 없음)
     */
    private BigDecimal calculateControversyScore(int approvalCount, int disapprovalCount,
                                                 int abstentionCount, int totalVotes) {
        if (totalVotes == 0) {
            return BigDecimal.ZERO;
        }

        // 최다 의견 찾기
        int maxVotes = Math.max(approvalCount, Math.max(disapprovalCount, abstentionCount));

        // 최다 의견의 비율
        BigDecimal maxRate = BigDecimal.valueOf(maxVotes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalVotes), 2, RoundingMode.HALF_UP);

        // 논란 지수 = 100 - 최다 의견 비율
        return BigDecimal.valueOf(100).subtract(maxRate);
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
