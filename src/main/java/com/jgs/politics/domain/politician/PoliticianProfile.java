package com.jgs.politics.domain.politician;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 의원의 투표 활동을 분석한 프로필
 * 
 * 매일 새벽 2시, 매주 월요일 새벽 3시에 자동으로 업데이트됨
 * VoteUpdateService에서 분석 계산 후 저장
 */
@Entity
@Table(name = "politician_profile")
@Getter
@Setter
@NoArgsConstructor
public class PoliticianProfile {

    @Id
    private String monaCd;  // 국회의원코드 (Politician.monaCd와 동일)

    private String hgNm;                    // 의원명
    private String polyNm;                  // 정당명

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer totalVotes;             // 참여한 투표 총 횟수

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer approvalCount;          // 찬성 횟수

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer disapprovalCount;       // 반대 횟수

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer abstentionCount;        // 기권 횟수

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal approvalRate;        // 찬성률 (%)

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal disapprovalRate;     // 반대률 (%)

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal abstentionRate;      // 기권률 (%)

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal partyConsistency;    // 정당 충성도 (%)
                                            // = 정당 주류 입장과 일치한 투표 수 / 전체 투표 수

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public PoliticianProfile(String monaCd, String hgNm, String polyNm, 
                            Integer totalVotes, Integer approvalCount, 
                            Integer disapprovalCount, Integer abstentionCount,
                            BigDecimal approvalRate, BigDecimal disapprovalRate,
                            BigDecimal abstentionRate, BigDecimal partyConsistency) {
        this.monaCd = monaCd;
        this.hgNm = hgNm;
        this.polyNm = polyNm;
        this.totalVotes = totalVotes != null ? totalVotes : 0;
        this.approvalCount = approvalCount != null ? approvalCount : 0;
        this.disapprovalCount = disapprovalCount != null ? disapprovalCount : 0;
        this.abstentionCount = abstentionCount != null ? abstentionCount : 0;
        this.approvalRate = approvalRate != null ? approvalRate : BigDecimal.ZERO;
        this.disapprovalRate = disapprovalRate != null ? disapprovalRate : BigDecimal.ZERO;
        this.abstentionRate = abstentionRate != null ? abstentionRate : BigDecimal.ZERO;
        this.partyConsistency = partyConsistency != null ? partyConsistency : BigDecimal.ZERO;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
