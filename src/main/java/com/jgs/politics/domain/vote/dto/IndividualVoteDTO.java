package com.jgs.politics.domain.vote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor // 이 어노테이션이 빌더 작동에 필수입니다
@NoArgsConstructor
public class IndividualVoteDTO {
    private String billId;      // 법안 ID (상세 페이지 이동용)
    private String billName;    // 법안명
    private String resultVote;  // 찬성/반대/기권/불참
    private String voteDate;    // 투표 일자
}