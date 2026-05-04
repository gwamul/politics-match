package com.jgs.politics.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PartyCentroidDTO {
    private String partyName;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal cohesion;
    private long memberCount;
}
