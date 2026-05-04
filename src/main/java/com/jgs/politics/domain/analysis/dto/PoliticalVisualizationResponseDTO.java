package com.jgs.politics.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PoliticalVisualizationResponseDTO {
    private String xAxisLabel;
    private String yAxisLabel;
    private String bubbleLabel;
    private List<PoliticalVisualizationPointDTO> points;
    private List<PartyCentroidDTO> parties;
    private String description;
}
