package com.jgs.politics.domain.vote.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoteStatDTO {
    private String monaCd;      // 의원 고유 코드
    private String hgNm;        // 이름
    private String polyNm;      // 정당
    private int yesCount;       // 찬성 횟수
    private int noCount;        // 반대 횟수
    private int abstentionCount; // 기권 횟수
    private double participationRate; // 표결 참여율
}