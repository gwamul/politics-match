package com.jgs.politics.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WeightedNominateItemDTO {
    private String monaCd;
    private String hgNm;
    private String polyNm;
    private BigDecimal weightedStanceScore;
    private BigDecimal partyConsistencyScore;
    private BigDecimal activityScore;
    private BigDecimal finalScore;
    private BigDecimal xAxis;
    private BigDecimal yAxis;
    private BigDecimal bubbleSize;
    private Integer totalVotes;
    private Integer controversialVotes;
}
