package com.jgs.politics.domain.vote.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberVoteHistoryResponse {
    private String hgNm;
    private String polyNm;
    private String monaCd;
    private List<IndividualVoteDTO> votes; // 해당 의원의 투표 기록 리스트
}