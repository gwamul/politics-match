package com.jgs.politics.domain.vote.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteResponse {
    private String age;        // 대 (22)
    private String hgNm;       // 의원명
    private String polyNm;     // 정당
    private String billName;   // 법안명
    private String resultVote; // 찬성/반대/기권
    private String voteDate;   // 투표일자
}