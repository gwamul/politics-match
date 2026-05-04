package com.jgs.politics.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PoliticalVisualizationPointDTO {
    private String monaCd;
    private String hgNm;
    private String polyNm;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal size;
    private BigDecimal score;
}
