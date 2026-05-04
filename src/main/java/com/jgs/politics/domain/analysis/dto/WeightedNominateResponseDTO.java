package com.jgs.politics.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WeightedNominateResponseDTO {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<WeightedNominateItemDTO> items;
    private String algorithmName;
    private String description;
}
