package com.jgs.politics.domain.analysis;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_sync_state")
@Getter
@Setter
@NoArgsConstructor
public class AnalysisSyncState {

    @Id
    private String stateKey;

    private Long lastProcessedVoteHistoryId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public AnalysisSyncState(String stateKey, Long lastProcessedVoteHistoryId) {
        this.stateKey = stateKey;
        this.lastProcessedVoteHistoryId = lastProcessedVoteHistoryId;
    }
}