package com.jgs.politics.domain.vote.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillVoteSummaryResponse {
    private String billId;
    private String billName;
    private long totalVoters;
    private Map<String, Long> counts; // {"찬성": 180, "반대": 100, ...}
    private Map<String, List<MemberIdentityDTO>> memberIdentities; 
}