package com.jgs.politics.domain.analysis.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jgs.politics.domain.analysis.dto.PartyCentroidDTO;
import com.jgs.politics.domain.analysis.dto.PoliticalVisualizationPointDTO;
import com.jgs.politics.domain.analysis.dto.PoliticalVisualizationResponseDTO;
import com.jgs.politics.domain.analysis.dto.WeightedNominateItemDTO;
import com.jgs.politics.domain.analysis.dto.WeightedNominateResponseDTO;
import com.jgs.politics.domain.bill.BillAnalysis;
import com.jgs.politics.domain.bill.repository.BillAnalysisRepository;
import com.jgs.politics.domain.politician.PartyAnalysis;
import com.jgs.politics.domain.politician.PoliticianProfile;
import com.jgs.politics.domain.politician.repository.PartyAnalysisRepository;
import com.jgs.politics.domain.politician.repository.PoliticianProfileRepository;
import com.jgs.politics.domain.vote.VoteHistory;
import com.jgs.politics.domain.vote.repository.VoteHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PoliticalRecommendationService {

    private static final BigDecimal DEFAULT_STANCE_WEIGHT = new BigDecimal("0.50");
    private static final BigDecimal DEFAULT_CONSISTENCY_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal DEFAULT_ACTIVITY_WEIGHT = new BigDecimal("0.20");

    private final PoliticianProfileRepository politicianProfileRepository;
    private final PartyAnalysisRepository partyAnalysisRepository;
    private final BillAnalysisRepository billAnalysisRepository;
    private final VoteHistoryRepository voteHistoryRepository;

    public WeightedNominateResponseDTO getWeightedNominate(int page,
                                                           int size,
                                                           BigDecimal stanceWeight,
                                                           BigDecimal consistencyWeight,
                                                           BigDecimal activityWeight) {
        List<WeightedNominateItemDTO> ranked = calculateRanking(stanceWeight, consistencyWeight, activityWeight);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, ranked.size());
        int toIndex = Math.min(fromIndex + safeSize, ranked.size());

        return WeightedNominateResponseDTO.builder()
                .page(safePage)
                .size(safeSize)
                .totalElements(ranked.size())
                .totalPages((int) Math.ceil(ranked.size() / (double) safeSize))
                .items(ranked.subList(fromIndex, toIndex))
                .algorithmName("Weighted Nominate")
                .description("안건 논란도, 의원 성향, 정당 일치도를 가중합해 추천 순위를 계산합니다.")
                .build();
    }

    public PoliticalVisualizationResponseDTO getVisualization(BigDecimal stanceWeight,
                                                              BigDecimal consistencyWeight,
                                                              BigDecimal activityWeight) {
        List<WeightedNominateItemDTO> ranked = calculateRanking(stanceWeight, consistencyWeight, activityWeight);

        List<PoliticalVisualizationPointDTO> points = ranked.stream()
                .map(item -> PoliticalVisualizationPointDTO.builder()
                        .monaCd(item.getMonaCd())
                        .hgNm(item.getHgNm())
                        .polyNm(item.getPolyNm())
                        .x(item.getXAxis())
                        .y(item.getYAxis())
                        .size(item.getBubbleSize())
                        .score(item.getFinalScore())
                        .build())
                .toList();

        Map<String, List<WeightedNominateItemDTO>> groupedByParty = ranked.stream()
                .collect(Collectors.groupingBy(WeightedNominateItemDTO::getPolyNm));

        List<PartyCentroidDTO> parties = new ArrayList<>();
        for (Map.Entry<String, List<WeightedNominateItemDTO>> entry : groupedByParty.entrySet()) {
            String partyName = entry.getKey();
            List<WeightedNominateItemDTO> items = entry.getValue();
            if (items.isEmpty()) {
                continue;
            }

            BigDecimal avgX = average(items.stream().map(WeightedNominateItemDTO::getXAxis).filter(Objects::nonNull).toList());
            BigDecimal avgY = average(items.stream().map(WeightedNominateItemDTO::getYAxis).filter(Objects::nonNull).toList());
            PartyAnalysis partyAnalysis = partyAnalysisRepository.findByPartyName(partyName).orElse(null);

            parties.add(PartyCentroidDTO.builder()
                    .partyName(partyName)
                    .x(avgX)
                    .y(avgY)
                    .cohesion(partyAnalysis != null && partyAnalysis.getCohesion() != null ? partyAnalysis.getCohesion() : BigDecimal.ZERO)
                    .memberCount(items.size())
                    .build());
        }

        return PoliticalVisualizationResponseDTO.builder()
                .xAxisLabel("좌/우 성향 축")
                .yAxisLabel("정당 일치도 축")
                .bubbleLabel("활동량")
                .points(points)
                .parties(parties)
                .description("의원별 위치는 가중 투표 성향과 정당 일치도를 기준으로 산출했습니다.")
                .build();
    }

    private List<WeightedNominateItemDTO> calculateRanking(BigDecimal stanceWeight,
                                                           BigDecimal consistencyWeight,
                                                           BigDecimal activityWeight) {
        List<PoliticianProfile> profiles = politicianProfileRepository.findAll();
        if (profiles.isEmpty()) {
            return List.of();
        }

        BigDecimal[] normalizedWeights = normalizeWeights(stanceWeight, consistencyWeight, activityWeight);
        BigDecimal normalizedStanceWeight = normalizedWeights[0];
        BigDecimal normalizedConsistencyWeight = normalizedWeights[1];
        BigDecimal normalizedActivityWeight = normalizedWeights[2];

        Map<String, BigDecimal> billControversyByBillId = billAnalysisRepository.findAll().stream()
                .collect(Collectors.toMap(
                        BillAnalysis::getBillId,
                        bill -> bill.getControversyScore() == null ? BigDecimal.ZERO : bill.getControversyScore(),
                        (left, right) -> left
                ));

        List<VoteHistory> allVotes = voteHistoryRepository.findAll();
        Map<String, List<VoteHistory>> votesByPolitician = allVotes.stream()
                .collect(Collectors.groupingBy(VoteHistory::getMonaCd));

        int maxVotes = profiles.stream()
                .map(PoliticianProfile::getTotalVotes)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        List<WeightedNominateItemDTO> ranking = new ArrayList<>();
        for (PoliticianProfile profile : profiles) {
            List<VoteHistory> votes = votesByPolitician.getOrDefault(profile.getMonaCd(), List.of());
            WeightedMetrics metrics = calculateMetrics(profile, votes, billControversyByBillId, maxVotes);

            BigDecimal finalScore = normalizedStanceWeight.multiply(metrics.normalizedStance())
                    .add(normalizedConsistencyWeight.multiply(metrics.consistencyScore()))
                    .add(normalizedActivityWeight.multiply(metrics.activityScore()));

            ranking.add(WeightedNominateItemDTO.builder()
                    .monaCd(profile.getMonaCd())
                    .hgNm(profile.getHgNm())
                    .polyNm(profile.getPolyNm())
                    .weightedStanceScore(metrics.weightedStance())
                    .partyConsistencyScore(metrics.consistencyScore())
                    .activityScore(metrics.activityScore())
                    .finalScore(finalScore.setScale(4, RoundingMode.HALF_UP))
                    .xAxis(metrics.weightedStance().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .yAxis(metrics.consistencyScore().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .bubbleSize(metrics.activityScore().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .totalVotes(profile.getTotalVotes() == null ? 0 : profile.getTotalVotes())
                    .controversialVotes(metrics.controversialVotes())
                    .build());
        }

        ranking.sort(Comparator.comparing(WeightedNominateItemDTO::getFinalScore).reversed()
                .thenComparing(WeightedNominateItemDTO::getTotalVotes, Comparator.reverseOrder()));
        return ranking;
    }

    private WeightedMetrics calculateMetrics(PoliticianProfile profile,
                                             List<VoteHistory> votes,
                                             Map<String, BigDecimal> billControversyByBillId,
                                             int maxVotes) {
        if (profile == null) {
            return new WeightedMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, 0);
        }

        BigDecimal weightedNumerator = BigDecimal.ZERO;
        BigDecimal weightDenominator = BigDecimal.ZERO;
        int controversialVotes = 0;

        for (VoteHistory vote : votes) {
            BigDecimal controversy = billControversyByBillId.getOrDefault(vote.getBillId(), BigDecimal.ZERO);
            BigDecimal voteWeight = BigDecimal.ONE.add(controversy.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            int sign = switch (vote.getResultVote()) {
                case "찬성" -> 1;
                case "반대" -> -1;
                default -> 0;
            };

            if (controversy.compareTo(new BigDecimal("50")) >= 0) {
                controversialVotes++;
            }

            weightedNumerator = weightedNumerator.add(voteWeight.multiply(BigDecimal.valueOf(sign)));
            weightDenominator = weightDenominator.add(voteWeight);
        }

        BigDecimal weightedStance = weightDenominator.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : weightedNumerator.divide(weightDenominator, 4, RoundingMode.HALF_UP);

        BigDecimal normalizedStance = weightedStance.add(BigDecimal.ONE)
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);

        BigDecimal consistencyScore = scaleOrZero(profile.getPartyConsistency());
        BigDecimal activityScore = maxVotes == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(profile.getTotalVotes() == null ? 0 : profile.getTotalVotes())
                .divide(BigDecimal.valueOf(maxVotes), 4, RoundingMode.HALF_UP);

        return new WeightedMetrics(weightedStance, normalizedStance, consistencyScore, activityScore, controversialVotes);
    }

    private BigDecimal[] normalizeWeights(BigDecimal stanceWeight,
                                          BigDecimal consistencyWeight,
                                          BigDecimal activityWeight) {
        BigDecimal s = stanceWeight == null ? DEFAULT_STANCE_WEIGHT : stanceWeight;
        BigDecimal c = consistencyWeight == null ? DEFAULT_CONSISTENCY_WEIGHT : consistencyWeight;
        BigDecimal a = activityWeight == null ? DEFAULT_ACTIVITY_WEIGHT : activityWeight;
        BigDecimal total = s.add(c).add(a);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal[] { DEFAULT_STANCE_WEIGHT, DEFAULT_CONSISTENCY_WEIGHT, DEFAULT_ACTIVITY_WEIGHT };
        }

        return new BigDecimal[] {
                s.divide(total, 4, RoundingMode.HALF_UP),
                c.divide(total, 4, RoundingMode.HALF_UP),
                a.divide(total, 4, RoundingMode.HALF_UP)
        };
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private record WeightedMetrics(BigDecimal weightedStance,
                                   BigDecimal normalizedStance,
                                   BigDecimal consistencyScore,
                                   BigDecimal activityScore,
                                   int controversialVotes) {
    }
}
