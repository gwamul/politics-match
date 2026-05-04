package com.jgs.politics.domain.bill;

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
 * 안건별 투표 패턴 분석
 * 
 * 안건에 대해 얼마나 의견이 갈렸는지, 어느 정당이 주로 찬성/반대했는지 분석
 * 매일 새벽 2시, 매주 월요일 새벽 3시에 자동으로 업데이트됨
 */
@Entity
@Table(name = "bill_analysis")
@Getter
@Setter
@NoArgsConstructor
public class BillAnalysis {

    @Id
    private String billId;                  // 의안ID (VoteHistory.billId와 동일)

    @Column(columnDefinition = "VARCHAR(500)")
    private String billName;                // 의안명

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer totalVotes;             // 투표한 의원 수

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer approvalCount;          // 찬성 의원 수

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer disapprovalCount;       // 반대 의원 수

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer abstentionCount;        // 기권 의원 수

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal approvalRate;        // 찬성률 (%)

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal disapprovalRate;     // 반대률 (%)

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal controversyScore;    // 논란 지수 (0~100)
                                            // = 100 - (max(찬성, 반대, 기권) / 총 투표수 * 100)
                                            // 높을수록 의견이 갈림

    private String voteDate;                // 투표일자

    private String passingStatus;           // 처리 결과 (가결/부결/기타)

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public BillAnalysis(String billId, String billName, Integer totalVotes,
                       Integer approvalCount, Integer disapprovalCount, Integer abstentionCount,
                       BigDecimal approvalRate, BigDecimal disapprovalRate,
                       BigDecimal controversyScore, String voteDate, String passingStatus) {
        this.billId = billId;
        this.billName = billName;
        this.totalVotes = totalVotes != null ? totalVotes : 0;
        this.approvalCount = approvalCount != null ? approvalCount : 0;
        this.disapprovalCount = disapprovalCount != null ? disapprovalCount : 0;
        this.abstentionCount = abstentionCount != null ? abstentionCount : 0;
        this.approvalRate = approvalRate != null ? approvalRate : BigDecimal.ZERO;
        this.disapprovalRate = disapprovalRate != null ? disapprovalRate : BigDecimal.ZERO;
        this.controversyScore = controversyScore != null ? controversyScore : BigDecimal.ZERO;
        this.voteDate = voteDate;
        this.passingStatus = passingStatus;
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
