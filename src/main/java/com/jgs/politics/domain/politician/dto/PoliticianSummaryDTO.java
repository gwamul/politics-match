package com.jgs.politics.domain.politician.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PoliticianSummaryDTO {
    private String monaCd;
    private String hgNm;
    private String polyNm;
    private String cityName;
    private String regionName;
    private String reeleGbnNm;
    private String photoUrl;
    private String blngCmitNm;
}