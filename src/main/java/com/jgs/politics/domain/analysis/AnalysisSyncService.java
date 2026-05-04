package com.jgs.politics.domain.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jgs.politics.domain.bill.BillAnalysis;
import com.jgs.politics.domain.bill.BillAnalysisRepository;
import com.jgs.politics.domain.politician.PartyAnalysis;
import com.jgs.politics.domain.politician.PartyAnalysisRepository;
import com.jgs.politics.domain.politician.PoliticianProfile;
import com.jgs.politics.domain.politician.PoliticianProfileRepository;
import com.jgs.politics.domain.vote.VoteHistory;
import com.jgs.politics.domain.vote.VoteHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisSyncService {

    private static final String DEFAULT_STATE_KEY = "MAIN";

    private final VoteHistoryRepository voteHistoryRepository;
    private final PoliticianProfileRepository politicianProfileRepository;
    private final PartyAnalysisRepository partyAnalysisRepository;
    private final BillAnalysisRepository billAnalysisRepository;
    private final AnalysisSyncStateRepository analysisSyncStateRepository;

    @Transactional
    public void syncAnalysis() {
        AnalysisSyncState syncState = analysisSyncStateRepository.findById(DEFAULT_STATE_KEY)
                .orElseGet(() -> new AnalysisSyncState(DEFAULT_STATE_KEY, 0L));

        boolean summaryTablesEmpty = politicianProfileRepository.count() == 0
            && partyAnalysisRepository.count() == 0
            && billAnalysisRepository.count() == 0;

        boolean needsRebuild = syncState.getLastProcessedVoteHistoryId() == null
                || syncState.getLastProcessedVoteHistoryId() == 0L;

        if ((needsRebuild && !summaryTablesEmpty) || (summaryTablesEmpty && syncState.getLastProcessedVoteHistoryId() != null && syncState.getLastProcessedVoteHistoryId() > 0L)) {
            rebuildAll(syncState);
            return;
        }

        Long lastProcessedVoteHistoryId = syncState.getLastProcessedVoteHistoryId();
        if (lastProcessedVoteHistoryId == null) {
            lastProcessedVoteHistoryId = 0L;
        }

        List<VoteHistory> newVotes = voteHistoryRepository.findByIdGreaterThanOrderByIdAsc(lastProcessedVoteHistoryId);
        if (newVotes.isEmpty()) {
            log.info("▶ [분석 동기화] 신규 투표 데이터가 없습니다. 마지막 처리 id={}", lastProcessedVoteHistoryId);
            return;
        }

        syncIncremental(newVotes, syncState);
    }

    private void rebuildAll(AnalysisSyncState syncState) {
        log.info("▶ [분석 동기화] 기존 분석 결과를 초기화하고 전체 재구성합니다.");
        politicianProfileRepository.deleteAll();
        partyAnalysisRepository.deleteAll();
        billAnalysisRepository.deleteAll();
        syncState.setLastProcessedVoteHistoryId(0L);
        analysisSyncStateRepository.save(syncState);

        List<VoteHistory> allVotes = voteHistoryRepository.findAll();
        if (allVotes.isEmpty()) {
            log.warn("▶ [분석 동기화] 원본 투표 데이터가 없어 전체 재구성을 건너뜁니다.");
            return;
        }

        syncIncremental(allVotes, syncState);
    }

    private void syncIncremental(List<VoteHistory> votesToProcess, AnalysisSyncState syncState) {
        Map<String, List<VoteHistory>> votesByBill = votesToProcess.stream()
                .collect(Collectors.groupingBy(VoteHistory::getBillId));

        Map<String, List<VoteHistory>> votesByParty = votesToProcess.stream()
                .collect(Collectors.groupingBy(VoteHistory::getPolyNm));

        Map<String, List<VoteHistory>> votesByPolitician = votesToProcess.stream()
                .collect(Collectors.groupingBy(VoteHistory::getMonaCd));

        Set<String> affectedParties = new HashSet<>(votesByParty.keySet());
        long maxVoteId = votesToProcess.stream()
                .map(VoteHistory::getId)
                .max(Comparator.naturalOrder())
                .orElse(syncState.getLastProcessedVoteHistoryId() == null ? 0L : syncState.getLastProcessedVoteHistoryId());

        updateBills(votesByBill);
        updateParties(votesByParty);
        updatePoliticians(votesByPolitician, affectedParties);
        recalculatePartyDerivedMetrics(affectedParties);

        syncState.setLastProcessedVoteHistoryId(maxVoteId);
        analysisSyncStateRepository.save(syncState);

        log.info("✔ [분석 동기화] {}건 처리 완료. lastProcessedVoteHistoryId={}", votesToProcess.size(), maxVoteId);
    }

    private void updateBills(Map<String, List<VoteHistory>> votesByBill) {
        for (Map.Entry<String, List<VoteHistory>> entry : votesByBill.entrySet()) {
            String billId = entry.getKey();
            List<VoteHistory> votes = entry.getValue();
            VoteHistory sample = votes.get(0);

            int approvalDelta = (int) votes.stream().filter(v -> "찬성".equals(v.getResultVote())).count();
            int disapprovalDelta = (int) votes.stream().filter(v -> "반대".equals(v.getResultVote())).count();
            int abstentionDelta = (int) votes.stream().filter(v -> "기권".equals(v.getResultVote())).count();
            int totalDelta = votes.size();

            BillAnalysis analysis = billAnalysisRepository.findByBillId(billId)
                    .orElseGet(() -> BillAnalysis.builder()
                            .billId(billId)
                            .billName(sample.getBillName())
                            .voteDate(sample.getVoteDate())
                            .passingStatus("동수")
                            .build());

            analysis.setBillName(sample.getBillName() != null ? sample.getBillName() : analysis.getBillName());
            analysis.setVoteDate(sample.getVoteDate() != null ? sample.getVoteDate() : analysis.getVoteDate());
            analysis.setTotalVotes(safeInt(analysis.getTotalVotes()) + totalDelta);
            analysis.setApprovalCount(safeInt(analysis.getApprovalCount()) + approvalDelta);
            analysis.setDisapprovalCount(safeInt(analysis.getDisapprovalCount()) + disapprovalDelta);
            analysis.setAbstentionCount(safeInt(analysis.getAbstentionCount()) + abstentionDelta);
            analysis.setApprovalRate(percentage(analysis.getApprovalCount(), analysis.getTotalVotes()));
            analysis.setDisapprovalRate(percentage(analysis.getDisapprovalCount(), analysis.getTotalVotes()));
            analysis.setControversyScore(controversyScore(analysis.getApprovalCount(), analysis.getDisapprovalCount(), analysis.getAbstentionCount(), analysis.getTotalVotes()));
            analysis.setPassingStatus(determinePassingStatus(analysis.getApprovalCount(), analysis.getDisapprovalCount()));

            billAnalysisRepository.save(analysis);
        }
    }

    private void updateParties(Map<String, List<VoteHistory>> votesByParty) {
        for (Map.Entry<String, List<VoteHistory>> entry : votesByParty.entrySet()) {
            String partyName = entry.getKey();
            List<VoteHistory> votes = entry.getValue();

            int approvalDelta = (int) votes.stream().filter(v -> "찬성".equals(v.getResultVote())).count();
            int disapprovalDelta = (int) votes.stream().filter(v -> "반대".equals(v.getResultVote())).count();
            int abstentionDelta = (int) votes.stream().filter(v -> "기권".equals(v.getResultVote())).count();
            int totalDelta = votes.size();

            PartyAnalysis analysis = partyAnalysisRepository.findByPartyName(partyName)
                    .orElseGet(() -> PartyAnalysis.builder()
                            .partyName(partyName)
                            .cohesion(BigDecimal.ZERO)
                            .build());

            analysis.setMemberCount(analysis.getMemberCount() == null ? 0 : analysis.getMemberCount());
            analysis.setTotalVotes(safeInt(analysis.getTotalVotes()) + totalDelta);
            analysis.setApprovalCount(safeInt(analysis.getApprovalCount()) + approvalDelta);
            analysis.setDisapprovalCount(safeInt(analysis.getDisapprovalCount()) + disapprovalDelta);
            analysis.setAbstentionCount(safeInt(analysis.getAbstentionCount()) + abstentionDelta);
            analysis.setAvgApprovalRate(percentage(analysis.getApprovalCount(), analysis.getTotalVotes()));
            analysis.setAvgDisapprovalRate(percentage(analysis.getDisapprovalCount(), analysis.getTotalVotes()));

            partyAnalysisRepository.save(analysis);
        }
    }

    private void updatePoliticians(Map<String, List<VoteHistory>> votesByPolitician, Set<String> affectedParties) {
        for (Map.Entry<String, List<VoteHistory>> entry : votesByPolitician.entrySet()) {
            String monaCd = entry.getKey();
            List<VoteHistory> votes = entry.getValue();
            VoteHistory sample = votes.get(0);

            int approvalDelta = (int) votes.stream().filter(v -> "찬성".equals(v.getResultVote())).count();
            int disapprovalDelta = (int) votes.stream().filter(v -> "반대".equals(v.getResultVote())).count();
            int abstentionDelta = (int) votes.stream().filter(v -> "기권".equals(v.getResultVote())).count();
            int totalDelta = votes.size();

            PoliticianProfile profile = politicianProfileRepository.findByMonaCd(monaCd)
                    .orElseGet(() -> PoliticianProfile.builder()
                            .monaCd(monaCd)
                            .hgNm(sample.getHgNm())
                            .polyNm(sample.getPolyNm())
                            .build());

            profile.setHgNm(sample.getHgNm() != null ? sample.getHgNm() : profile.getHgNm());
            profile.setPolyNm(sample.getPolyNm() != null ? sample.getPolyNm() : profile.getPolyNm());
            profile.setTotalVotes(safeInt(profile.getTotalVotes()) + totalDelta);
            profile.setApprovalCount(safeInt(profile.getApprovalCount()) + approvalDelta);
            profile.setDisapprovalCount(safeInt(profile.getDisapprovalCount()) + disapprovalDelta);
            profile.setAbstentionCount(safeInt(profile.getAbstentionCount()) + abstentionDelta);
            profile.setApprovalRate(percentage(profile.getApprovalCount(), profile.getTotalVotes()));
            profile.setDisapprovalRate(percentage(profile.getDisapprovalCount(), profile.getTotalVotes()));
            profile.setAbstentionRate(percentage(profile.getAbstentionCount(), profile.getTotalVotes()));

            politicianProfileRepository.save(profile);
            if (profile.getPolyNm() != null) {
                affectedParties.add(profile.getPolyNm());
            }
        }
    }

    private void recalculatePartyDerivedMetrics(Set<String> affectedParties) {
        for (String partyName : affectedParties) {
            PartyAnalysis partyAnalysis = partyAnalysisRepository.findByPartyName(partyName).orElse(null);
            if (partyAnalysis == null) {
                continue;
            }

            List<PoliticianProfile> profiles = politicianProfileRepository.findByPolyNm(partyName);
            partyAnalysis.setMemberCount(profiles.size());

            String majorityVote = majorityVote(partyAnalysis.getApprovalCount(), partyAnalysis.getDisapprovalCount(), partyAnalysis.getAbstentionCount());
            if (majorityVote == null) {
                partyAnalysis.setCohesion(BigDecimal.ZERO);
                partyAnalysisRepository.save(partyAnalysis);
                continue;
            }

            for (PoliticianProfile profile : profiles) {
                profile.setPartyConsistency(percentage(matchingCount(profile, majorityVote), safeInt(profile.getTotalVotes())));
                politicianProfileRepository.save(profile);
            }

            List<PoliticianProfile> refreshedProfiles = politicianProfileRepository.findByPolyNm(partyName);
            BigDecimal cohesion = refreshedProfiles.isEmpty()
                    ? BigDecimal.ZERO
                    : refreshedProfiles.stream()
                    .map(PoliticianProfile::getPartyConsistency)
                    .filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(refreshedProfiles.size()), 2, RoundingMode.HALF_UP);
            partyAnalysis.setCohesion(cohesion);
            partyAnalysisRepository.save(partyAnalysis);
        }
    }

    private int matchingCount(PoliticianProfile profile, String majorityVote) {
        if (majorityVote == null) {
            return 0;
        }
        return switch (majorityVote) {
            case "찬성" -> safeInt(profile.getApprovalCount());
            case "반대" -> safeInt(profile.getDisapprovalCount());
            case "기권" -> safeInt(profile.getAbstentionCount());
            default -> 0;
        };
    }

    private String majorityVote(int approvalCount, int disapprovalCount, int abstentionCount) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("찬성", approvalCount);
        counts.put("반대", disapprovalCount);
        counts.put("기권", abstentionCount);
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private BigDecimal controversyScore(int approvalCount, int disapprovalCount, int abstentionCount, int totalVotes) {
        if (totalVotes == 0) {
            return BigDecimal.ZERO;
        }
        int maxVotes = Math.max(approvalCount, Math.max(disapprovalCount, abstentionCount));
        BigDecimal maxRate = percentage(maxVotes, totalVotes);
        return BigDecimal.valueOf(100).subtract(maxRate);
    }

    private BigDecimal percentage(int count, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
    
    private String determinePassingStatus(int approvalCount, int disapprovalCount) {
        if (approvalCount > disapprovalCount) return "가결";
        if (disapprovalCount > approvalCount) return "부결";
        return "동수";
    }
}