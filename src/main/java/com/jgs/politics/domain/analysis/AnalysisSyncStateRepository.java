package com.jgs.politics.domain.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisSyncStateRepository extends JpaRepository<AnalysisSyncState, String> {
}