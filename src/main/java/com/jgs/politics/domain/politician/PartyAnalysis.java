package com.jgs.politics.domain.politician;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정당별 투표 패턴 분석
 * 
 * 매일 새벽 2시, 매주 월요일 새벽 3시에 자동으로 업데이트됨
 */
@Entity
@Table(name = "party_analysis")
@Getter
@Setter
@NoArgsConstructor
public class PartyAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String partyName;               // 정당명

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer memberCount;            // 해당 정당 의원 수

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer totalVotes;             // 해당 정당의 누적 투표 횟수

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer approvalCount;          // 찬성 투표 총합

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer disapprovalCount;       // 반대 투표 총합

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer abstentionCount;        // 기권 투표 총합

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal avgApprovalRate;     // 정당 평균 찬성률 (%)

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal avgDisapprovalRate;  // 정당 평균 반대률 (%)

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal cohesion;            // 응집도 (%)
                                            // = 정당 내 의원들의 투표 일관성
                                            // 높을수록 의견이 일치함

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public PartyAnalysis(String partyName, Integer memberCount, Integer totalVotes,
                        Integer approvalCount, Integer disapprovalCount, Integer abstentionCount,
                        BigDecimal avgApprovalRate, BigDecimal avgDisapprovalRate,
                        BigDecimal cohesion) {
        this.partyName = partyName;
        this.memberCount = memberCount != null ? memberCount : 0;
        this.totalVotes = totalVotes != null ? totalVotes : 0;
        this.approvalCount = approvalCount != null ? approvalCount : 0;
        this.disapprovalCount = disapprovalCount != null ? disapprovalCount : 0;
        this.abstentionCount = abstentionCount != null ? abstentionCount : 0;
        this.avgApprovalRate = avgApprovalRate != null ? avgApprovalRate : BigDecimal.ZERO;
        this.avgDisapprovalRate = avgDisapprovalRate != null ? avgDisapprovalRate : BigDecimal.ZERO;
        this.cohesion = cohesion != null ? cohesion : BigDecimal.ZERO;
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
