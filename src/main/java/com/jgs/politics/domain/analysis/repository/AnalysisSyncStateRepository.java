package com.jgs.politics.domain.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jgs.politics.domain.analysis.AnalysisSyncState;

@Repository
public interface AnalysisSyncStateRepository extends JpaRepository<AnalysisSyncState, String> {
}