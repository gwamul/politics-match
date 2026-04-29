package com.jgs.politics.domain.politician;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Politician {

    @Id
    @JsonProperty("NAAS_CD")
    private String monaCd;

    @JsonProperty("NAAS_NM")
    private String hgNm;

    @JsonProperty("NAAS_CH_NM")
    private String hjNm;

    @JsonProperty("NAAS_EN_NM")
    private String engNm;

    @JsonProperty("BIRDY_DIV_CD")
    private String bthDiv;

    @JsonProperty("BIRDY_DT")
    private String bthDate;

    @JsonProperty("DTY_NM")
    private String jobNm;

    @JsonProperty("PLPT_NM")
    private String polyNm;

    @JsonProperty("ELECD_NM")
    private String origNm;
    
    private String regionName;
    
    private String cityName;

    @JsonProperty("ELECD_DIV_NM")
    private String origDiv;

    @JsonProperty("CMIT_NM")
    @Column(columnDefinition = "TEXT")
    private String cmitNm;

    @JsonProperty("BLNG_CMIT_NM")
    @Column(columnDefinition = "TEXT")
    private String blngCmitNm;

    @JsonProperty("RLCT_DIV_NM")
    private String reeleGbnNm;

    @JsonProperty("GTELT_ERACO")
    private String units;

    @JsonProperty("NTR_DIV")
    private String sexGbnNm;

    @JsonProperty("NAAS_TEL_NO")
    private String telNo;

    @JsonProperty("NAAS_EMAIL_ADDR")
    private String eMail;

    @JsonProperty("NAAS_HP_URL")
    private String homepage;

    @JsonProperty("AIDE_NM")
    @Column(columnDefinition = "TEXT")
    private String staff;

    @JsonProperty("CHF_SCRT_NM")
    @Column(columnDefinition = "TEXT")
    private String secretary;

    @JsonProperty("SCRT_NM")
    @Column(columnDefinition = "TEXT")
    private String subSecretary;

    @JsonProperty("BRF_HST")
    @Column(columnDefinition = "TEXT")
    private String history;

    @JsonProperty("OFFM_RNUM_NO")
    private String officeRoom;

    @JsonProperty("NAAS_PIC")
    private String photoUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;
}