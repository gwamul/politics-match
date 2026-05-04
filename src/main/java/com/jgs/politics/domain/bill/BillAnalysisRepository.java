package com.jgs.politics.domain.bill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface BillAnalysisRepository extends JpaRepository<BillAnalysis, String> {
    
    Optional<BillAnalysis> findByBillId(String billId);
    
    List<BillAnalysis> findAllByOrderByControversyScoreDesc();
    
    List<BillAnalysis> findTop10ByOrderByControversyScoreDesc();
}
