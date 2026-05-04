package com.jgs.politics.domain.vote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor // Builder를 쓰려면 반드시 필요
@NoArgsConstructor
public class MemberIdentityDTO {
    private String hgNm;
    private String monaCd; // 고유 식별자
    private String polyNm; // 정당까지 보여주면 완벽한 구분 가능
}